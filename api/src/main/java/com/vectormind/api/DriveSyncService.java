package com.vectormind.api;

import com.vectormind.api.config.WeaviateConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.google.api.client.http.GenericUrl;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;

@Service
public class DriveSyncService {

    private static final String APP = "VectorMind";
    private static final GsonFactory JSON = GsonFactory.getDefaultInstance();
    private static final int BATCH_SIZE = 10;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Value("${google.client.id}")
    private String CLIENT_ID;

    @Value("${google.client.secret}")
    private String CLIENT_SECRET;

    private final DriveTokenRepository repo;
    private final RestTemplate rest;
    private final DocumentReferenceRepository docRefRepo;
    private final WeaviateConfig weaviateConfig;
    private final String weaviateApiKey;

    public DriveSyncService(
        DriveTokenRepository repo,
        RestTemplate rest,
        DocumentReferenceRepository docRefRepo,
        WeaviateConfig weaviateConfig,
        @Value("${weaviate.api-key:}") String weaviateApiKey
    ) {
        this.repo = repo;
        this.rest = rest;
        this.docRefRepo = docRefRepo;
        this.weaviateConfig = weaviateConfig;
        this.weaviateApiKey = weaviateApiKey;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public GoogleAuthorizationCodeFlow flow() throws Exception {
        return new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON,
            CLIENT_ID,
            CLIENT_SECRET,
            List.of(DriveScopes.DRIVE_READONLY)
        )
        .setAccessType("offline")
        .build();
    }

    public Credential exchange(String code) throws Exception {
        GoogleTokenResponse res = flow()
            .newTokenRequest(code)
            .setRedirectUri(redirectUri)
            .execute();
        return flow().createAndStoreCredential(res, null);
    }

    public void sync(String userId) {
        repo.findByUserId(userId).ifPresent(tok -> {
            try {
                Drive drive = buildDriveService(tok);
                List<File> files = drive.files().list()
                    .setFields("files(id,name,mimeType,size)")
                    .setQ("mimeType='application/pdf'")
                    .setPageSize(100)
                    .execute()
                    .getFiles();

                for (int i = 0; i < files.size(); i += BATCH_SIZE) {
                    List<File> batch = files.subList(i, Math.min(i+BATCH_SIZE, files.size()));
                    for (File f : batch) {
                        processFile(drive, f, userId);
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processFile(Drive drive, File file, String userId) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        drive.files().get(file.getId()).executeMediaAndDownloadTo(os);
        byte[] content = os.toByteArray();
        String rawText;
        try (PDDocument pdf = PDDocument.load(content)) {
            rawText = new PDFTextStripper().getText(pdf);
        }
        String docId = UUID.randomUUID().toString();
        ingestText(rawText, file.getName(), docId, "default", userId, "drive");
        DocumentReference ref = new DocumentReference(docId, userId, file.getName(), file.getId(), "drive");
        ref.setFileSize(file.getSize());
        docRefRepo.save(ref);
        os.close();
    }

    private void ingestText(
        String rawText,
        String filename,
        String docId,
        String workspace,
        String userId,
        String source
    ) {
        try {
            List<String> chunks = chunkText(rawText, 400);
            
            // Skip embedding service - use random vectors temporarily
            List<List<Double>> vectors = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                List<Double> vector = new ArrayList<>();
                for (int j = 0; j < 384; j++) {
                    vector.add(Math.random());
                }
                vectors.add(vector);
            }

            HttpHeaders weavHdr = new HttpHeaders();
            weavHdr.setContentType(MediaType.APPLICATION_JSON);
            if (weaviateApiKey != null && !weaviateApiKey.isEmpty()) {
                weavHdr.set("Authorization", "Bearer " + weaviateApiKey);
            }

            for (int i = 0; i < chunks.size(); i++) {
                Map<String,Object> obj = Map.of(
                    "class","Chunk",
                    "id", UUID.randomUUID().toString(),
                    "properties", Map.of(
                        "docId", docId,
                        "text", chunks.get(i),
                        "page", i+1,
                        "userId", userId
                    ),
                    "vector", vectors.get(i)
                );
                rest.postForEntity(
                    weaviateConfig.getObjectsEndpoint(),
                    new HttpEntity<>(obj, weavHdr),
                    String.class
                );
            }

            rest.postForEntity(
                weaviateConfig.getObjectsEndpoint(),
                new HttpEntity<>(Map.of(
                    "class","Document",
                    "id", docId,
                    "properties", Map.of(
                        "title", filename,
                        "pages", chunks.size(),
                        "processed", true,
                        "workspace", workspace,
                        "userId", userId,
                        "source", source
                    )
                ), weavHdr),
                String.class
            );
            
            System.out.println("[DriveSyncService] Successfully ingested " + chunks.size() + " chunks for " + filename);
        } catch (Exception e) {
            System.err.println("[DriveSyncService] Ingestion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public byte[] downloadFileContent(String googleDriveId, String userId) {
        return repo.findByUserId(userId).map(tok -> {
            try {
                Drive drive = buildDriveService(tok);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                drive.files().get(googleDriveId).executeMediaAndDownloadTo(os);
                return os.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).orElse(null);
    }

    private Drive buildDriveService(DriveToken tok) throws Exception {
        Credential cred = new Credential.Builder(
            com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod()
        )
        .setTransport(GoogleNetHttpTransport.newTrustedTransport())
        .setJsonFactory(JSON)
        .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
        .setClientAuthentication(
            new ClientParametersAuthentication(CLIENT_ID, CLIENT_SECRET)
        )
        .build();

        cred.setAccessToken(tok.getAccessToken());
        cred.setRefreshToken(tok.getRefreshToken());
        cred.setExpirationTimeMilliseconds(tok.getExpiryTime().toEpochMilli());

        if (cred.getExpirationTimeMilliseconds() != null &&
            cred.getExpirationTimeMilliseconds() <= System.currentTimeMillis()) {
            cred.refreshToken();
            tok.setAccessToken(cred.getAccessToken());
            tok.setExpiryTime(Instant.ofEpochMilli(cred.getExpirationTimeMilliseconds()));
            repo.save(tok);
        }

        return new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON,
            cred
        )
        .setApplicationName(APP)
        .build();
    }

    private List<String> chunkText(String text, int max) {
        String[] w = text.split("\\s+");
        List<String> o = new ArrayList<>();
        for (int i = 0; i < w.length; i += max) {
            o.add(String.join(" ",
                Arrays.copyOfRange(w, i, Math.min(i + max, w.length))
            ));
        }
        return o;
    }
}