// 4. DocumentCacheService.java - New caching service
package com.vectormind.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentCacheService {
    
    // In-memory cache for frequently accessed documents
    private final Cache<String, byte[]> documentCache;
    
    private final DriveSyncService driveSyncService;
    private final DocumentReferenceRepository referenceRepo;
    
    public DocumentCacheService(DriveSyncService driveSyncService,
                               DocumentReferenceRepository referenceRepo) {
        this.driveSyncService = driveSyncService;
        this.referenceRepo = referenceRepo;
        
        // Configure cache: max 100 documents, expire after 1 hour
        this.documentCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    }
    
    public byte[] getDocument(String docId, String userId) {
        // Check cache first
        byte[] cached = documentCache.getIfPresent(docId);
        if (cached != null) {
            System.out.println("[Cache] Hit for document: " + docId);
            updateAccessMetrics(docId);
            return cached;
        }
        
        System.out.println("[Cache] Miss for document: " + docId);
        
        // Not in cache, fetch from source
        DocumentReference ref = referenceRepo.findByDocIdAndUserId(docId, userId)
            .orElse(null);
            
        if (ref == null) {
            return null;
        }
        
        byte[] content = null;
        
        try {
            if ("drive".equals(ref.getSource())) {
                // Fetch from Google Drive
                content = driveSyncService.downloadFileContent(ref.getGoogleDriveId(), userId);
            } else {
                // Fetch from local storage (uploaded files)
                java.nio.file.Path path = java.nio.file.Paths.get("uploads", docId + ".pdf");
                if (java.nio.file.Files.exists(path)) {
                    content = java.nio.file.Files.readAllBytes(path);
                }
            }
            
            if (content != null) {
                // Store in cache for next time
                documentCache.put(docId, content);
                updateAccessMetrics(docId);
            }
            
        } catch (Exception e) {
            System.err.println("[Cache] Failed to fetch document: " + e.getMessage());
        }
        
        return content;
    }
    
    private void updateAccessMetrics(String docId) {
        referenceRepo.findById(docId).ifPresent(ref -> {
            ref.setLastAccessed(java.time.Instant.now());
            ref.setAccessCount(ref.getAccessCount() + 1);
            referenceRepo.save(ref);
        });
    }
    
    public void evictDocument(String docId) {
        documentCache.invalidate(docId);
    }
}