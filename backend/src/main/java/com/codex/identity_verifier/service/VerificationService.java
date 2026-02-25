package com.codex.identity_verifier.service;

import com.codex.identity_verifier.dto.VerificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

@Service
public class VerificationService {

    /**
     * Mock verification logic that simulates OCR processing and document analysis
     * TODO: Integrate with AWS Textract for real OCR processing
     * TODO: Add OpenCV for image validation and tampering detection
     */
    public VerificationResponse verifyDocument(MultipartFile file) {
        try {
            // Simulate processing delay
            Thread.sleep(1000);
            
            // Generate SHA-256 hash of the file content
            String fileHash = generateSHA256(file.getBytes());
            
            // Generate mock OCR data
            String[] names = {"Demo User", "John Smith", "Jane Doe", "Alex Johnson", "Sarah Williams"};
            String[] idNumbers = {"XXXX1234", "YYYY5678", "ZZZZ9012", "AAAA3456", "BBBB7890"};
            String[] dobs = {"01-01-1990", "15-03-1985", "22-07-1992", "10-12-1988", "05-09-1995"};
            
            Random random = new Random();
            String mockName = names[random.nextInt(names.length)];
            String mockIdNumber = idNumbers[random.nextInt(idNumbers.length)];
            String mockDob = dobs[random.nextInt(dobs.length)];
            
            // Calculate risk score (0-100)
            int riskScore = random.nextInt(100);
            String riskLevel;
            
            if (riskScore < 30) {
                riskLevel = "LOW RISK";
            } else if (riskScore < 70) {
                riskLevel = "MEDIUM RISK";
            } else {
                riskLevel = "HIGH RISK";
            }
            
            // Generate explanation
            String[] explanations = {
                "Mock tampering check",
                "Document authenticity verified",
                "ID number format validation passed",
                "Image quality assessment: Good",
                "OCR confidence level: High"
            };
            
            return VerificationResponse.builder()
                    .riskLevel(riskLevel)
                    .riskScore(riskScore)
                    .explanation(Arrays.asList(explanations))
                    .extractedData(VerificationResponse.ExtractedData.builder()
                            .name(mockName)
                            .idNumber(mockIdNumber)
                            .dob(mockDob)
                            .build())
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("Verification failed", e);
        }
    }

    private String generateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}