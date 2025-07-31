package com.vectormind.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DocumentCacheService {

    private final Cache<String, byte[]> documentCache;
    private final DocumentReferenceRepository referenceRepo;
    private final DriveSyncService driveSyncService;

    public DocumentCacheService(DocumentReferenceRepository referenceRepo,
                                DriveSyncService driveSyncService) {
        this.referenceRepo      = referenceRepo;
        this.driveSyncService   = driveSyncService;
        this.documentCache      = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(1000)
            .build();
    }

    /** Fetches from cache or loads via DriveSyncService.downloadFileContent(...) */
    public byte[] getDocument(String docId, String userId) {
        return documentCache.get(docId, id -> {
            byte[] content = driveSyncService.downloadFileContent(id, userId);
            updateAccessMetrics(id);
            return content;
        });
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
