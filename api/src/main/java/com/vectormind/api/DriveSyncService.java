package com.vectormind.api;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

  @Value("${upload.external.endpoint}")
  private String UPLOAD_URL;

  private final DriveTokenRepository repo;
  private final RestTemplate rest;
  private final DocumentReferenceRepository docRefRepo;

  public DriveSyncService(DriveTokenRepository repo,
                          RestTemplate rest,
                          DocumentReferenceRepository docRefRepo) {
    this.repo = repo;
    this.rest = rest;
    this.docRefRepo = docRefRepo;
  }

  /** used by DriveController **/
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
    GoogleTokenResponse res = flow().newTokenRequest(code)
                                    .setRedirectUri(redirectUri)
                                    .execute();
    return flow().createAndStoreCredential(res, null);
  }

  public void sync(String userId) {
    repo.findByUserId(userId).ifPresentOrElse(tok -> {
      try {
        Drive drive = buildDriveService(tok);
        List<File> files = drive.files().list()
          .setFields("files(id,name,mimeType,size)")
          .setQ("mimeType='application/pdf'")
          .setPageSize(100)
          .execute().getFiles();

        for (int i=0;i<files.size();i+=BATCH_SIZE) {
          List<File> batch = files.subList(i, Math.min(i+BATCH_SIZE, files.size()));
          for (File f : batch) {
            processFile(drive,f,userId);
          }
          Thread.sleep(500);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, () -> System.err.println("No token for " + userId));
  }

  private Drive buildDriveService(DriveToken tok) throws Exception {
    Credential cred = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
      .setTransport(GoogleNetHttpTransport.newTrustedTransport())
      .setJsonFactory(JSON)
      .setTokenServerUrl(new com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"))
      .setClientAuthentication(new ClientParametersAuthentication(CLIENT_ID, CLIENT_SECRET))
      .build();
    cred.setAccessToken(tok.getAccessToken());
    cred.setRefreshToken(tok.getRefreshToken());
    cred.setExpirationTimeMilliseconds(tok.getExpiryTime().toEpochMilli());
    if (cred.getExpirationTimeMilliseconds() <= System.currentTimeMillis()) {
      cred.refreshToken();
      tok.setAccessToken(cred.getAccessToken());
      tok.setExpiryTime(Instant.ofEpochMilli(cred.getExpirationTimeMilliseconds()));
      repo.save(tok);
    }
    return new Drive.Builder(
      GoogleNetHttpTransport.newTrustedTransport(), JSON, cred
    ).setApplicationName(APP).build();
  }

  private void processFile(Drive drive, File file, String userId) throws Exception {
    var out = new ByteArrayOutputStream();
    drive.files().get(file.getId()).executeMediaAndDownloadTo(out);
    byte[] data = out.toByteArray();
    out.close();

    String rawText;
    try (var pdf = org.apache.pdfbox.pdmodel.PDDocument.load(data)) {
      rawText = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
    }

    String docId = UUID.randomUUID().toString();
    ingestText(rawText, file.getName(), docId, "default", userId, "drive");

    DocumentReference ref = new DocumentReference(docId, userId, file.getName(), file.getId(), "drive");
    ref.setFileSize(file.getSize());
    docRefRepo.save(ref);
  }

  /**
   * Used by DocumentCacheService
   */
  public byte[] downloadFileContent(String googleDriveId, String userId) {
    return repo.findByUserId(userId).map(tok -> {
      try {
        var out = new ByteArrayOutputStream();
        buildDriveService(tok).files().get(googleDriveId)
          .executeMediaAndDownloadTo(out);
        return out.toByteArray();
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }).orElse(null);
  }

  private void ingestText(String rawText,
                         String filename,
                         String docId,
                         String workspace,
                         String userId,
                         String source) {
    try {
      List<String> chunks = chunkText(rawText,400);

      // OpenAI embeddings
      String openAiKey = System.getenv("OPENAI_API_KEY");
      HttpHeaders embH = new HttpHeaders();
      embH.setContentType(MediaType.APPLICATION_JSON);
      embH.setBearerAuth(openAiKey);
      Map<String,Object> embReq = Map.of("model","text-embedding-ada-002","input",chunks);
      @SuppressWarnings("unchecked")
      Map<?,?> embResp = rest.postForEntity(
        "https://api.openai.com/v1/embeddings",
        new HttpEntity<>(embReq,embH),
        Map.class
      ).getBody();
      @SuppressWarnings("unchecked")
      List<Map<String,Object>> data = (List<Map<String,Object>>)embResp.get("data");
      List<List<Double>> vectors = new ArrayList<>();
      for (var item : data) {
        @SuppressWarnings("unchecked")
        List<Double> v = (List<Double>) item.get("embedding");
        vectors.add(v);
      }

      // Weaviate store
      String weav = System.getenv("WEAVIATE_URL");
      if (!weav.startsWith("http")) weav="https://"+weav;
      String endpoint = weav + "/v1/objects";
      HttpHeaders jsonH = new HttpHeaders();
      jsonH.setContentType(MediaType.APPLICATION_JSON);
      String weavKey = System.getenv("WEAVIATE_API_KEY");
      if (weavKey!=null&&!weavKey.isBlank()) jsonH.set("X-API-KEY",weavKey);

      for (int i=0;i<chunks.size();i++) {
        Map<String,Object> obj = Map.of(
          "class","Chunk",
          "id",UUID.randomUUID().toString(),
          "properties",Map.of(
            "docId", docId,
            "text",  chunks.get(i),
            "page",  i+1,
            "userId",userId
          ),
          "vector", vectors.get(i)
        );
        rest.postForEntity(endpoint,new HttpEntity<>(obj,jsonH),String.class);
      }

      Map<String,Object> docObj = Map.of(
        "class","Document",
        "id",docId,
        "properties",Map.of(
          "title",     filename,
          "pages",     chunks.size(),
          "processed", true,
          "workspace", workspace,
          "userId",    userId,
          "source",    source
        )
      );
      rest.postForEntity(endpoint,new HttpEntity<>(docObj,jsonH),String.class);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<String> chunkText(String text, int max) {
    String[] w = text.split("\\s+");
    List<String> out = new ArrayList<>();
    for (int i=0;i<w.length;i+=max) {
      out.add(String.join(" ",
        Arrays.copyOfRange(w,i,Math.min(i+max,w.length))
      ));
    }
    return out;
  }
}
