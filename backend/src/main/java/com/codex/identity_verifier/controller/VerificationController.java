package com.codex.identity_verifier.controller;

import com.codex.identity_verifier.dto.VerificationResponse;
import com.codex.identity_verifier.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyDocument(@RequestParam("file") MultipartFile file) {
        // Validate file presence
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Validate file type (basic validation)
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only images and PDFs are allowed.");
        }

        // Process the file using the service
        VerificationResponse response = verificationService.verifyDocument(file);
        return ResponseEntity.ok(response);
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
            contentType.startsWith("image/") || 
            contentType.equals("application/pdf")
        );
    }
}