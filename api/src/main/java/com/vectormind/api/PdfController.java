package com.vectormind.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final DocumentReferenceRepository documentReferenceRepository;
    private final Path uploadDir = Paths.get("uploads");

    @Autowired
    public PdfController(DocumentReferenceRepository documentReferenceRepository) {
        this.documentReferenceRepository = documentReferenceRepository;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    @GetMapping("/{docId}")
    @CrossOrigin(origins = {"https://dociq.tech", "http://localhost:5173", "http://localhost:3000"}, 
                 allowCredentials = "true")
    public ResponseEntity<Resource> getPdf(
            @PathVariable String docId,
            Authentication auth) {
        
        try {
            String userId = getUserId(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Verify the document belongs to the user
            var docRef = documentReferenceRepository.findByDocIdAndUserId(docId, userId);
            if (docRef.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Construct the file path
            Path filePath = uploadDir.resolve(docId + ".pdf");
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Create resource
            Resource resource = new FileSystemResource(file);

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/pdf";
            }

            // Return the file with appropriate headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + docRef.get().getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, 
                            CacheControl.maxAge(1, TimeUnit.HOURS).getHeaderValue())
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}