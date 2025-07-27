// src/main/java/com/vectormind/api/DriveController.java
package com.vectormind.api;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drive")
public class DriveController {

    private final DriveSyncService sync;
    private final DriveTokenRepository repo;

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

        // ✅ Dynamic frontend redirect
        String frontendRedirect = System.getenv().getOrDefault("FRONTEND_REDIRECT_URI", "http://localhost:5173");
        System.out.println("✅ FRONTEND_REDIRECT_URI used: " + frontendRedirect);

        return ResponseEntity.status(302)
            .location(URI.create(frontendRedirect + "/dashboard?drive=connected&temp=" + tempKey))
            .build();
    }

    @PostMapping("/claim")
    public ResponseEntity<Boolean> claimToken(@RequestParam("tempKey") String tempKey,
                                              Authentication auth) {
        String userId = getUserId(auth);
        return repo.findByUserId(tempKey).map(tempToken -> {
            // move token from tempKey to real userId
            var userToken = new DriveToken(
                userId,
                tempToken.getAccessToken(),
                tempToken.getRefreshToken(),
                tempToken.getExpiryTime()
            );
            repo.save(userToken);
            repo.delete(tempToken);

            // NEW: kick off full Drive sync immediately
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
