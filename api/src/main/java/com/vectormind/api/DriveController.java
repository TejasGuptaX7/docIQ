package com.vectormind.api;

import com.google.api.client.auth.oauth2.Credential;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/drive")
public class DriveController {

    private final DriveSyncService sync;
    private final DriveTokenRepository repo;

    public DriveController(DriveSyncService sync, DriveTokenRepository repo) {
        this.sync = sync;
        this.repo = repo;
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() throws Exception {
        String url = sync.flow()
                .newAuthorizationUrl()
                .setRedirectUri("http://localhost:8082/api/drive/oauth2callback")
                .build();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/oauth2callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code) throws Exception {
        String userId = "demo"; // Mock for now

        Credential c = sync.exchange(code);
        repo.findByUserId(userId).ifPresentOrElse(
            existing -> {
                existing.setAccessToken(c.getAccessToken());
                existing.setRefreshToken(c.getRefreshToken());
                existing.setExpiryTime(Instant.ofEpochMilli(c.getExpirationTimeMilliseconds()));
                repo.save(existing);
            },
            () -> repo.save(new DriveToken(
                userId,
                c.getAccessToken(),
                c.getRefreshToken(),
                Instant.ofEpochMilli(c.getExpirationTimeMilliseconds())
            ))
        );

        new Thread(() -> sync.sync(userId)).start();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/dashboard?drive=connected"))
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> status() {
        String userId = "demo"; // Mock for now
        return ResponseEntity.ok(repo.findByUserId(userId).isPresent());
    }
}
