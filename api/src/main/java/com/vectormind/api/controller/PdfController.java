package com.vectormind.api.controller; // Make sure this matches your package structure

import com.vectormind.api.DocumentReferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller // Changed from @RestController to be explicit
@ResponseBody 
@CrossOrigin(origins = {"https://dociq.tech", "https://api.dociq.tech", "http://localhost:5173", "http://localhost:3000"}, 
             allowCredentials = "true",
             methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class PdfController {

    private final DocumentReferenceRepository documentReferenceRepository;
    private final Path uploadDir = Paths.get("uploads");
    
    @Autowired(required = false) // Make optional to avoid startup issues
    private JwtDecoder jwtDecoder;

    @Autowired
    public PdfController(DocumentReferenceRepository documentReferenceRepository) {
        this.documentReferenceRepository = documentReferenceRepository;
    }

    private String getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            return jwt.getSubject();
        }
        return null;
    }

    @GetMapping("/api/pdf/{docId}")  // Full path including /api
    public ResponseEntity<Resource> getPdf(
            @PathVariable String docId,
            @RequestParam(required = false) String token,
            Authentication auth) {
        
        try {
            String userId = getUserId(auth);
            
            // If no auth and token provided, try to decode it
            if (userId == null && token != null && !token.isEmpty() && jwtDecoder != null) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    userId = jwt.getSubject();
                } catch (Exception e) {
                    // Log but don't fail - token might be invalid
                    System.err.println("Failed to decode token: " + e.getMessage());
                }
            }
            
            // For now, if still no userId, return unauthorized
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
            }

            // Verify the document belongs to the user
            var docRef = documentReferenceRepository.findByDocIdAndUserId(docId, userId);
            if (docRef.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
            }

            // Construct the file path
            Path filePath = uploadDir.resolve(docId + ".pdf");
            File file = filePath.toFile();
            
            if (!file.exists()) {
                System.err.println("File not found: " + filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
            }

            // Create resource
            Resource resource = new FileSystemResource(file);

            // Return the file with appropriate headers
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + docRef.get().getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
    
    // Add a test endpoint to verify the controller is loaded
    @GetMapping("/api/pdf/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("PDF Controller is working");
    }
}