package com.vectormind.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
    private JwtDecoder jwtDecoder; // Add JWT decoder for token validation

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
            @RequestParam(required = false) String token,
            Authentication auth) {
        
        try {
            String userId = null;
            
            // First try to get userId from Authentication
            userId = getUserId(auth);
            
            // If no auth, try to decode token from query parameter
            if (userId == null && token != null && !token.isEmpty()) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    userId = jwt.getSubject();
                } catch (Exception e) {
                    // Invalid token
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            }
            
            // If still no userId, unauthorized
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

            // Return the file with appropriate headers for browser viewing
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + docRef.get().getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, 
                            "no-cache, no-store, must-revalidate")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}