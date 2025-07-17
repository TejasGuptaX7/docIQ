package com.vectormind.api;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpRequestInitializer;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;

@Service
public class DriveSyncService {

  private static final String APP = "VectorMind";
  private static final GsonFactory JSON = GsonFactory.getDefaultInstance();
  private static final int BATCH_SIZE = 10; // Process files in batches

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
  private final DocumentReferenceRepository docRefRepo; // NEW

  public DriveSyncService(DriveTokenRepository repo, 
                         RestTemplate rest,
                         DocumentReferenceRepository docRefRepo) {
    this.repo = repo;
    this.rest = rest;
    this.docRefRepo = docRefRepo;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  /* Build flow for step-1 (redirect). */
  public GoogleAuthorizationCodeFlow flow() throws Exception {
    return new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON,
            CLIENT_ID,
            CLIENT_SECRET,
            List.of(DriveScopes.DRIVE_READONLY))
        .setAccessType("offline")
        .build();
  }

  /* Exchange ?code=… for Credential */
  public Credential exchange(String code) throws Exception {
    GoogleTokenResponse res = flow().newTokenRequest(code)
                                    .setRedirectUri(redirectUri)
                                    .execute();
    return flow().createAndStoreCredential(res, null);
  }

  /* Enhanced sync method - processes files without storing them locally */
  public void sync(String userId) {
    System.out.println("[DriveSyncService] Starting sync for user: " + userId);
    
    repo.findByUserId(userId).ifPresentOrElse(tok -> {
      try {
        System.out.println("[DriveSyncService] Found token for user: " + userId);
        
        Drive drive = buildDriveService(tok);

        System.out.println("[DriveSyncService] Requesting PDF files from Google Drive...");
        
        Drive.Files.List req = drive.files().list()
            .setFields("files(id,name,webContentLink,mimeType,size)")
            .setQ("mimeType='application/pdf'")
            .setPageSize(100);

        List<File> files = req.execute().getFiles();
        System.out.println("[DriveSyncService] Found " + files.size() + " PDF files in Drive");
        
        int processedCount = 0;
        int failedCount = 0;
        
        // Process files in batches to avoid memory issues
        for (int i = 0; i < files.size(); i += BATCH_SIZE) {
            List<File> batch = files.subList(i, Math.min(i + BATCH_SIZE, files.size()));
            System.out.println("[DriveSyncService] Processing batch " + (i/BATCH_SIZE + 1) + 
                             " of " + ((files.size() + BATCH_SIZE - 1) / BATCH_SIZE));
            
            for (File f : batch) {
                try {
                    processFile(drive, f, userId);
                    processedCount++;
                } catch (Exception e) {
                    System.err.println("[DriveSyncService] Failed to process file: " + 
                                     f.getName() + " - " + e.getMessage());
                    failedCount++;
                }
            }
            
            // Small delay between batches to avoid rate limiting
            Thread.sleep(500);
        }
        
        System.out.println("[DriveSyncService] Sync completed. Processed " + processedCount + 
                         " files, Failed " + failedCount + " files");
        
      } catch (Exception e) { 
        System.err.println("[DriveSyncService] Sync failed for user: " + userId);
        e.printStackTrace(); 
      }
    }, () -> {
      System.err.println("[DriveSyncService] No token found for user: " + userId);
    });
  }

  /* Process individual file without storing to disk */
  private void processFile(Drive drive, File file, String userId) throws Exception {
    System.out.println("[DriveSyncService] Processing file: " + file.getName() + 
                     " (ID: " + file.getId() + ", Size: " + file.getSize() + " bytes)");
    
    // Skip very small files
    if (file.getSize() != null && file.getSize() < 100) {
      System.out.println("[DriveSyncService] Skipping file (too small): " + file.getName());
      return;
    }
    
    // Download file to memory
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    drive.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
    byte[] fileContent = outputStream.toByteArray();
    
    System.out.println("[DriveSyncService] Downloaded " + fileContent.length + " bytes");
    
    // Extract text for search
    String rawText;
    try (org.apache.pdfbox.pdmodel.PDDocument pdf = 
         org.apache.pdfbox.pdmodel.PDDocument.load(fileContent)) {
      rawText = new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
      System.out.println("[DriveSyncService] Extracted " + rawText.length() + " characters");
    }
    
    // Generate unique document ID
    String docId = UUID.randomUUID().toString();
    
    // Ingest text into Weaviate for search
    ingestText(rawText, file.getName(), docId, "default", userId, "drive");
    
    // Store reference for later retrieval (NOT the actual file)
    DocumentReference ref = new DocumentReference(
        docId, 
        userId, 
        file.getName(), 
        file.getId(), 
        "drive"
    );
    ref.setFileSize(file.getSize());
    docRefRepo.save(ref);
    
    System.out.println("[DriveSyncService] Successfully processed: " + file.getName() + 
                     " as docId: " + docId);
    
    // Let garbage collector clean up the file content
    outputStream.close();
  }

  /* Method to download file content on-demand (for cache service) */
  public byte[] downloadFileContent(String googleDriveId, String userId) {
    return repo.findByUserId(userId).map(tok -> {
      try {
        Drive drive = buildDriveService(tok);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        drive.files().get(googleDriveId).executeMediaAndDownloadTo(outputStream);
        return outputStream.toByteArray();
      } catch (Exception e) {
        System.err.println("[DriveSyncService] Failed to download file: " + e.getMessage());
        return null;
      }
    }).orElse(null);
  }

  /* Helper method to build Drive service */
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

    // Refresh token if needed
    if (cred.getExpirationTimeMilliseconds() != null && 
        cred.getExpirationTimeMilliseconds() <= System.currentTimeMillis()) {
      System.out.println("[DriveSyncService] Token expired, refreshing...");
      cred.refreshToken();
      
      tok.setAccessToken(cred.getAccessToken());
      tok.setExpiryTime(Instant.ofEpochMilli(cred.getExpirationTimeMilliseconds()));
      repo.save(tok);
    }

    return new Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(), JSON, cred)
        .setApplicationName(APP)
        .build();
  }
  
  /* Your existing ingestText method stays the same */
  private void ingestText(String rawText, String filename, String docId, 
                         String workspace, String userId, String source) {
    try {
      System.out.println("[DriveSyncService] Ingesting text for: " + filename);
      
      List<String> chunks = chunkText(rawText, 400);
      Map<String, Object> embedReq = Map.of("texts", chunks);

      HttpHeaders jsonHeaders = new HttpHeaders();
      jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

      // Get embeddings
      @SuppressWarnings("unchecked")
      List<List<Double>> vectors = (List<List<Double>>) rest
          .postForEntity("http://localhost:5001/embed", 
                        new HttpEntity<>(embedReq, jsonHeaders), Map.class)
          .getBody().get("embeddings");

      System.out.println("[DriveSyncService] Got embeddings for " + vectors.size() + " chunks");

      // Store chunks in Weaviate
      for (int i = 0; i < chunks.size(); i++) {
        Map<String, Object> obj = Map.of(
            "class", "Chunk",
            "id", UUID.randomUUID().toString(),
            "properties", Map.of(
                "docId", docId,
                "text", chunks.get(i),
                "page", i + 1,
                "userId", userId
            ),
            "vector", vectors.get(i)
        );
        rest.postForEntity("http://localhost:8080/v1/objects",
            new HttpEntity<>(obj, jsonHeaders), String.class);
      }

      // Store document metadata
      rest.postForEntity("http://localhost:8080/v1/objects",
          new HttpEntity<>(Map.of(
              "class", "Document",
              "id", docId,
              "properties", Map.of(
                  "title", filename,
                  "pages", chunks.size(),
                  "processed", true,
                  "workspace", workspace,
                  "userId", userId,
                  "source", source
              )
          ), jsonHeaders), String.class);
          
      System.out.println("[DriveSyncService] Successfully ingested " + chunks.size() + 
                        " chunks for " + filename);
    } catch (Exception e) {
      System.err.println("[DriveSyncService] Ingestion failed for " + filename + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /* Your existing chunkText method stays the same */
  private List<String> chunkText(String text, int maxTokens) {
    String[] words = text.split("\\s+");
    List<String> chunks = new ArrayList<>();
    for (int i = 0; i < words.length; i += maxTokens) {
      chunks.add(String.join(" ", 
        Arrays.copyOfRange(words, i, Math.min(i + maxTokens, words.length))));
    }
    return chunks;
  }
}