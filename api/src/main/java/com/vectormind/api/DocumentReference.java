package com.vectormind.api;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document_references")
public class DocumentReference {
    @Id
    private String docId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column
    private String googleDriveId; // null for uploaded files
    
    @Column(nullable = false)
    private String source; // "upload" or "drive"
    
    @Column
    private Long fileSize;
    
    @Column
    private Instant lastAccessed;
    
    @Column
    private Instant createdAt;
    
    @Column
    private Integer accessCount = 0;
    
    // Constructor
    public DocumentReference() {}
    
    public DocumentReference(String docId, String userId, String fileName, 
                           String googleDriveId, String source) {
        this.docId = docId;
        this.userId = userId;
        this.fileName = fileName;
        this.googleDriveId = googleDriveId;
        this.source = source;
        this.createdAt = Instant.now();
        this.accessCount = 0;
    }
    
    // Getters and setters
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getGoogleDriveId() { return googleDriveId; }
    public void setGoogleDriveId(String googleDriveId) { this.googleDriveId = googleDriveId; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Integer getAccessCount() { return accessCount; }
    public void setAccessCount(Integer accessCount) { this.accessCount = accessCount; }
}