// src/main/java/com/vectormind/api/DriveController.java
package com.vectormind.api;

import java.net.URI;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drive")
public class DriveController {

    private final DriveSyncService sync;
    private final DriveTokenRepository repo;

    @Value("${frontend.redirect.uri}")
    private String frontendRedirectUri;

    public DriveController(DriveSyncService sync, DriveTokenRepository repo) {
        this.sync = sync;
        this.repo = repo;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new RuntimeException("User not authenticated");
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() throws Exception {
        String url = sync.flow()
                         .newAuthorizationUrl()
                         .setRedirectUri(sync.getRedirectUri())
                         .build();
        return ResponseEntity.status(302)
                             .location(URI.create(url))
                             .build();
    }

    @GetMapping("/oauth2callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code) throws Exception {
        String tempKey = "temp_" + System.currentTimeMillis();
        var cred = sync.exchange(code);
        var tok  = new DriveToken(
            tempKey,
            cred.getAccessToken(),
            cred.getRefreshToken(),
            Instant.ofEpochMilli(cred.getExpirationTimeMilliseconds())
        );
        repo.save(tok);

        System.out.println("✅ FRONTEND_REDIRECT_URI (Spring) = " + frontendRedirectUri);

        return ResponseEntity.status(302)
            .location(URI.create(frontendRedirectUri + "/dashboard?drive=connected&temp=" + tempKey))
            .build();
    }

    @PostMapping("/claim")
    public ResponseEntity<Boolean> claimToken(@RequestParam("tempKey") String tempKey,
                                              Authentication auth) {
        String userId = getUserId(auth);
        return repo.findByUserId(tempKey).map(tempToken -> {
            var userToken = new DriveToken(
                userId,
                tempToken.getAccessToken(),
                tempToken.getRefreshToken(),
                tempToken.getExpiryTime()
            );
            repo.save(userToken);
            repo.delete(tempToken);

            new Thread(() -> sync.sync(userId)).start();

            return ResponseEntity.ok(true);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> status(Authentication auth) {
        String userId = getUserId(auth);
        boolean has = repo.findByUserId(userId).isPresent();
        return ResponseEntity.ok(has);
    }
}
