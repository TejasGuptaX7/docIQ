// src/main/java/com/vectormind/api/DocumentController.java
package com.vectormind.api;

import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final Path storage = Paths.get("uploads");

    @GetMapping("/{id}.pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable String id) {
        try {
            Path file = storage.resolve(id + ".pdf");
            if (!Files.exists(file)) return ResponseEntity.notFound().build();

            Resource res = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getFileName() + "\"")
                    .body(res);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
