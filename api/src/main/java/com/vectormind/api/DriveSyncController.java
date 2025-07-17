package com.vectormind.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/drive")
public class DriveSyncController {

    private final DriveSyncService syncService;

    public DriveSyncController(DriveSyncService syncService) {
        this.syncService = syncService;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new RuntimeException("Not authenticated");
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String,String>> syncDrive(Authentication auth) {
        String userId = getUserId(auth);
        // launches the sync process (blocking or async)
        syncService.sync(userId);
        return ResponseEntity.ok(Map.of("status", "sync started"));
    }
}
