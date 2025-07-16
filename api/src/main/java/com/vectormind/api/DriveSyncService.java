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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DriveSyncService {

    private static final String APP = "VectorMind";
    private static final GsonFactory JSON = GsonFactory.getDefaultInstance();

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

    public DriveSyncService(DriveTokenRepository repo, RestTemplate rest) {
        this.repo = repo;
        this.rest = rest;
    }

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

    public Credential exchange(String code) throws Exception {
        GoogleTokenResponse res = flow().newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();
        return flow().createAndStoreCredential(res, null);
    }

    public void sync(String userId) {
        repo.findByUserId(userId).ifPresent(tok -> {
            try {
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

                Drive drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON, cred)
                        .setApplicationName(APP)
                        .build();

                Drive.Files.List req = drive.files().list()
                        .setFields("files(id,name,webContentLink)")
                        .setPageSize(1000);

                List<File> files = req.execute().getFiles();
                for (File f : files) {
                    if (f.getWebContentLink() == null) continue;
                    rest.postForEntity(
                            UPLOAD_URL,
                            Map.of("url", f.getWebContentLink(),
                                   "name", f.getName(),
                                   "workspace", userId),
                            Void.class
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
