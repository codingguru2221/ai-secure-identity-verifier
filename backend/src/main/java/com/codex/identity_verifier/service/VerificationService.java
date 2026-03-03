package com.codex.identity_verifier.service;

import com.codex.identity_verifier.dto.VerificationResponse;
import com.codex.identity_verifier.model.VerificationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final S3Service s3Service;
    private final RekognitionService rekognitionService;
    private final TextractService textractService;
    private final DynamoDBService dynamoDBService;
    private final DataProtectionService dataProtectionService;
    private final FraudModelService fraudModelService;
    private final LambdaFraudScoringService lambdaFraudScoringService;

    @Value("${verification.delete-uploaded-file:true}")
    private boolean deleteUploadedFile;
    @Value("${verification.mask-sensitive-response:true}")
    private boolean maskSensitiveResponse;

    @Autowired
    public VerificationService(S3Service s3Service, RekognitionService rekognitionService,
                              TextractService textractService, DynamoDBService dynamoDBService,
                              DataProtectionService dataProtectionService, FraudModelService fraudModelService,
                              LambdaFraudScoringService lambdaFraudScoringService) {
        this.s3Service = s3Service;
        this.rekognitionService = rekognitionService;
        this.textractService = textractService;
        this.dynamoDBService = dynamoDBService;
        this.dataProtectionService = dataProtectionService;
        this.fraudModelService = fraudModelService;
        this.lambdaFraudScoringService = lambdaFraudScoringService;
    }

    /**
     * Performs comprehensive document verification using AWS services
     * @param file The document file to verify
     * @return VerificationResponse containing the verification results
     */
    public VerificationResponse verifyDocument(MultipartFile file, String ownerUsername) {
        String s3Key = null;
        try {
            // 1. Upload file to S3
            s3Key = s3Service.uploadFile(file);

            // 2. Download file bytes for local and AI processing
            byte[] imageData = s3Service.downloadFile(s3Key);
            String fileHash = dataProtectionService.sha256Hex(imageData);

            boolean isPdf = isPdfFile(file);
            boolean imageAnalysisEnabled = !isPdf;

            // 3. Image analysis engines
            Map<String, Object> faceDetectionResult = imageAnalysisEnabled
                    ? rekognitionService.detectFaces(imageData)
                    : defaultFaceDetectionResult();
            Map<String, Object> tamperDetectionResult = imageAnalysisEnabled
                    ? rekognitionService.detectImageTampering(imageData)
                    : defaultTamperDetectionResult();
            Map<String, Object> qualityAnalysisResult = imageAnalysisEnabled
                    ? rekognitionService.analyzeImageQuality(imageData)
                    : defaultQualityAnalysisResult();

            // 4. OCR extraction
            Map<String, String> identityInfo = textractService.extractIdentityInformation(imageData);
            String identitySignatureHash = buildIdentitySignatureHash(identityInfo);

            // 5. Duplicate detection (privacy preserving via hashes)
            long fileHashMatches = dynamoDBService.countByFileHash(fileHash);
            long identityMatches = dynamoDBService.countByIdentitySignatureHash(identitySignatureHash);
            boolean duplicateDetected = fileHashMatches > 0 || identityMatches > 0;
            int duplicateMatches = (int) Math.max(fileHashMatches, identityMatches);

            // 6. Risk scoring
            int riskScore = calculateRiskScore(
                    faceDetectionResult,
                    tamperDetectionResult,
                    qualityAnalysisResult,
                    identityInfo,
                    duplicateDetected,
                    duplicateMatches,
                    imageAnalysisEnabled
            );

            int identityConsistencyPenalty = evaluateIdentityConsistency(identityInfo);
            riskScore = Math.min(100, riskScore + identityConsistencyPenalty);
            if (duplicateDetected) {
                riskScore = Math.min(100, riskScore + Math.min(35, 15 + duplicateMatches * 5));
            }

            // Optional external model signal
            int modelRisk = fraudModelService.getAdditionalRiskScore(fileHash, determineRiskLevel(riskScore));
            riskScore = Math.min(100, riskScore + modelRisk);
            int tamperScore = ((Number) tamperDetectionResult.getOrDefault("tamperScore", 0)).intValue();
            int lambdaRisk = lambdaFraudScoringService.getAdditionalRiskScore(fileHash, identitySignatureHash, tamperScore, duplicateDetected);
            riskScore = Math.min(100, riskScore + lambdaRisk);
            String riskLevel = determineRiskLevel(riskScore);

            // 7. Build explainable output
            List<String> explanations = generateExplanations(
                    faceDetectionResult,
                    tamperDetectionResult,
                    qualityAnalysisResult,
                    identityInfo,
                    riskScore,
                    duplicateDetected,
                    duplicateMatches,
                    imageAnalysisEnabled
            );
            if (identityConsistencyPenalty > 0) {
                explanations.add("Cross-field validation: inconsistent extracted identity fields detected.");
            }
            if (duplicateDetected) {
                explanations.add("Duplicate detection: matched " + duplicateMatches + " prior verification(s) by hash/signature.");
            }
            if (modelRisk > 0) {
                explanations.add("External fraud model raised additional risk score by " + modelRisk + " points.");
            }
            if (lambdaRisk > 0) {
                explanations.add("AWS Lambda risk engine raised score by " + lambdaRisk + " points.");
            }
            if (isPdf) {
                explanations.add(0, "PDF detected: OCR extraction performed via Textract; image-only checks were skipped.");
            }

            Double highestConfidence = ((Number) faceDetectionResult.getOrDefault("highestConfidence", 0.0)).doubleValue();
            Boolean isTampered = Boolean.TRUE.equals(tamperDetectionResult.get("isTampered"));

            // 8. Persist record
            VerificationRecord verificationRecord = createVerificationRecord(
                    file.getOriginalFilename(),
                    s3Key,
                    riskLevel,
                    riskScore,
                    tamperScore,
                    duplicateDetected,
                    duplicateMatches,
                    explanations.toArray(new String[0]),
                    identityInfo,
                    highestConfidence,
                    isTampered,
                    fileHash,
                    identitySignatureHash,
                    ownerUsername
            );
            dynamoDBService.saveVerificationRecord(verificationRecord);

            // 9. Privacy-safe response
            return VerificationResponse.builder()
                    .riskLevel(riskLevel)
                    .riskScore(riskScore)
                    .explanation(explanations)
                    .extractedData(VerificationResponse.ExtractedData.builder()
                            .name(maskSensitiveResponse ? maskName(identityInfo.getOrDefault("name", "")) : identityInfo.getOrDefault("name", ""))
                            .idNumber(maskSensitiveResponse ? maskIdentifier(identityInfo.getOrDefault("idNumber", "")) : identityInfo.getOrDefault("idNumber", ""))
                            .dob(maskSensitiveResponse ? maskDob(identityInfo.getOrDefault("dob", "")) : identityInfo.getOrDefault("dob", ""))
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Verification failed", e);
            String message = e.getMessage() != null ? e.getMessage() : "Unknown verification error";
            throw new RuntimeException(message, e);
        } finally {
            if (deleteUploadedFile && s3Key != null) {
                try {
                    s3Service.deleteFile(s3Key);
                } catch (Exception cleanupException) {
                    log.warn("Failed to delete uploaded object from S3: {}", s3Key, cleanupException);
                }
            }
        }
    }

    /**
     * Calculates the overall risk score based on multiple analysis factors
     */
    private int calculateRiskScore(Map<String, Object> faceDetectionResult, 
                                  Map<String, Object> tamperDetectionResult,
                                  Map<String, Object> qualityAnalysisResult,
                                  Map<String, String> identityInfo,
                                  boolean duplicateDetected,
                                  int duplicateMatches,
                                  boolean imageAnalysisEnabled) {
        int baseScore = 0;
        
        if (imageAnalysisEnabled) {
            // Factor 1: Face detection analysis
            if (!(Boolean) faceDetectionResult.getOrDefault("isFaceDetected", false)) {
                baseScore += 35; // High risk if no face detected in ID photo
            } else {
                // Check face quality if face is detected
                Integer faceCount = (Integer) faceDetectionResult.get("faceCount");
                if (faceCount != null && faceCount > 1) {
                    baseScore += 15; // Multiple faces may indicate document issues
                }
            }
            
            // Factor 2: Tampering detection
            if ((Boolean) tamperDetectionResult.getOrDefault("isTampered", false)) {
                baseScore += 45;
            }
            int tamperScore = ((Number) tamperDetectionResult.getOrDefault("tamperScore", 0)).intValue();
            baseScore += Math.min(25, tamperScore / 3);
            if (tamperScore >= 70) {
                baseScore += 10;
            }
            
            // Factor 3: Image quality analysis
            @SuppressWarnings("unchecked")
            Map<String, Boolean> qualityIndicators = (Map<String, Boolean>) qualityAnalysisResult.getOrDefault("qualityIndicators", Map.of());
            if (qualityIndicators.getOrDefault("isBlurry", false)) {
                baseScore += 25; // Blur significantly increases risk
            }
            
            if (!qualityIndicators.getOrDefault("hasGoodLighting", false)) {
                baseScore += 10; // Poor lighting affects analysis accuracy
            }
            
            if (qualityIndicators.getOrDefault("isDocument", false)) {
                baseScore -= 15; // Document type decreases risk
            }
        }
        
        // Factor 4: Identity information completeness
        int missingFields = 0;
        if (identityInfo.get("name") == null || identityInfo.get("name").isEmpty()) {
            missingFields++;
            baseScore += 20; // Name is critical
        }
        if (identityInfo.get("idNumber") == null || identityInfo.get("idNumber").isEmpty()) {
            missingFields++;
            baseScore += 25; // ID number is very critical
        }
        if (identityInfo.get("dob") == null || identityInfo.get("dob").isEmpty()) {
            missingFields++;
            baseScore += 10; // DOB is important
        }
        
        // Bonus for complete information
        if (missingFields == 0) {
            baseScore -= 20; // All fields present
        }
        
        if (imageAnalysisEnabled) {
            // Factor 5: Suspicious content detection
            if ((Boolean) tamperDetectionResult.getOrDefault("hasSuspiciousContent", false)) {
                baseScore += 30;
            }
            
            // Factor 6: Image dimensions (too small/large can be suspicious)
            Integer width = (Integer) tamperDetectionResult.get("width");
            Integer height = (Integer) tamperDetectionResult.get("height");
            if (width != null && height != null) {
                int totalPixels = width * height;
                if (totalPixels < 100000) { // Very low resolution
                    baseScore += 20;
                } else if (totalPixels > 20000000) { // Extremely high resolution (suspicious)
                    baseScore += 10;
                }
            }
        }

        // Factor 7: Duplicate detection
        if (duplicateDetected) {
            baseScore += Math.min(25, 10 + (duplicateMatches * 3));
        }
        
        // Ensure score stays within bounds
        return Math.min(100, Math.max(0, baseScore));
    }

    /**
     * Determines the risk level based on the risk score
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore < 35) {
            return "VERIFIED";
        } else if (riskScore < 70) {
            return "SUSPICIOUS";
        } else {
            return "HIGH-RISK";
        }
    }

    /**
     * Generates explanations based on the analysis results
     */
    private List<String> generateExplanations(Map<String, Object> faceDetectionResult,
                                             Map<String, Object> tamperDetectionResult,
                                             Map<String, Object> qualityAnalysisResult,
                                             Map<String, String> identityInfo,
                                             int riskScore,
                                             boolean duplicateDetected,
                                             int duplicateMatches,
                                             boolean imageAnalysisEnabled) {
        List<String> explanations = new ArrayList<>();
        
        if (imageAnalysisEnabled) {
            // Face detection results
            Integer faceCount = (Integer) faceDetectionResult.get("faceCount");
            if ((Boolean) faceDetectionResult.getOrDefault("isFaceDetected", false)) {
                if (faceCount != null && faceCount > 1) {
                    explanations.add("Face detection: Multiple faces (" + faceCount + ") detected - may indicate document issues");
                } else {
                    explanations.add("Face detection: Single face detected - positive indicator");
                }
            } else {
                explanations.add("WARNING: No face detected in document - potential fraud indicator");
            }
            
            // Tampering results
            if ((Boolean) tamperDetectionResult.getOrDefault("isTampered", false)) {
                explanations.add("ALERT: Potential tampering detected in document");
            } else {
                explanations.add("Document integrity check: No obvious signs of tampering detected");
            }
            int tamperScore = ((Number) tamperDetectionResult.getOrDefault("tamperScore", 0)).intValue();
            explanations.add("Tamper score (0-100): " + tamperScore);
            @SuppressWarnings("unchecked")
            List<String> tamperSignals = (List<String>) tamperDetectionResult.getOrDefault("tamperSignals", List.of());
            if (!tamperSignals.isEmpty()) {
                explanations.add("Tamper signals: " + String.join(", ", tamperSignals));
            }
            
            // Quality results
            @SuppressWarnings("unchecked")
            Map<String, Boolean> qualityIndicators = (Map<String, Boolean>) qualityAnalysisResult.getOrDefault("qualityIndicators", Map.of());
            if (qualityIndicators.getOrDefault("isBlurry", false)) {
                explanations.add("Image quality assessment: Low quality detected - may affect analysis accuracy");
            } else {
                explanations.add("Image quality assessment: Good quality for analysis");
            }
            
            if (!qualityIndicators.getOrDefault("hasGoodLighting", false)) {
                explanations.add("Lighting conditions: Poor lighting detected - may impact OCR accuracy");
            }
        } else {
            explanations.add("Image-based face and tamper checks skipped for PDF input.");
        }
        
        // Identity info extraction
        if (identityInfo.get("name") != null && !identityInfo.get("name").isEmpty()) {
            explanations.add("Name extracted: " + maskName(identityInfo.get("name")));
        } else {
            explanations.add("Name extraction: Failed to extract name from document - critical information missing");
        }
        
        if (identityInfo.get("idNumber") != null && !identityInfo.get("idNumber").isEmpty()) {
            explanations.add("ID number extracted: " + maskIdentifier(identityInfo.get("idNumber")));
        } else {
            explanations.add("ID number extraction: Failed to extract ID number from document - critical information missing");
        }
        
        if (identityInfo.get("dob") != null && !identityInfo.get("dob").isEmpty()) {
            explanations.add("Date of birth extracted: " + maskDob(identityInfo.get("dob")));
        } else {
            explanations.add("Date of birth extraction: Failed to extract DOB from document");
        }

        if (duplicateDetected) {
            explanations.add("Duplicate pattern alert: " + duplicateMatches + " prior matching record(s) found.");
        } else {
            explanations.add("Duplicate pattern check: no prior matching hash/signature found.");
        }
        
        // Risk assessment
        if (riskScore >= 80) {
            explanations.add("Final risk assessment: HIGH-RISK - Strong indicators of potential fraud detected");
            explanations.add("RECOMMENDATION: Immediate manual review and additional verification required");
        } else if (riskScore >= 60) {
            explanations.add("Final risk assessment: SUSPICIOUS (High-Medium) - Significant concerns detected");
            explanations.add("RECOMMENDATION: Enhanced verification procedures recommended");
        } else if (riskScore >= 40) {
            explanations.add("Final risk assessment: SUSPICIOUS - Moderate concerns identified");
            explanations.add("RECOMMENDATION: Additional verification may be needed");
        } else if (riskScore >= 20) {
            explanations.add("Final risk assessment: VERIFIED with cautions - Minor concerns identified");
            explanations.add("RECOMMENDATION: Standard verification procedures sufficient");
        } else {
            explanations.add("Final risk assessment: VERIFIED - Document appears authentic and complete");
            explanations.add("RECOMMENDATION: Standard processing approved");
        }
        
        return explanations;
    }

    /**
     * Creates a verification record for storage in DynamoDB
     */
    private VerificationRecord createVerificationRecord(
        String fileName,
        String s3Key,
        String riskLevel,
        int riskScore,
        int tamperScore,
        boolean duplicateDetected,
        int duplicateMatches,
        String[] explanations,
        Map<String, String> identityInfo,
        Double faceMatchConfidence,
        Boolean isTampered,
        String fileHash,
        String identitySignatureHash,
        String ownerUsername) {

    VerificationRecord.ExtractedData extractedData =
            VerificationRecord.ExtractedData.builder()
                    .name(identityInfo.get("name"))
                    .idNumber(identityInfo.get("idNumber"))
                    .dob(identityInfo.get("dob"))
                    .address(identityInfo.get("address"))
                    .expiryDate(identityInfo.get("expiryDate"))
                    .build();

    // Convert String[] → List<String>
    List<String> explanationList = Arrays.asList(explanations);

    return VerificationRecord.builder()
            .fileName(fileName)
            .fileHash(fileHash)
            .identitySignatureHash(identitySignatureHash)
            .ownerUsername(ownerUsername)
            .s3Key(s3Key)
            .s3Bucket(s3Service.getBucketName()) // Use actual bucket name from S3Service
            .s3ObjectDeleted(deleteUploadedFile)
            .privacyMode(maskSensitiveResponse ? "MASKED_RESPONSE+ENCRYPTED_STORAGE" : "ENCRYPTED_STORAGE")
            .riskLevel(riskLevel)
            .riskScore(riskScore)
            .tamperScore(tamperScore)
            .duplicateDetected(duplicateDetected)
            .duplicateMatches(duplicateMatches)
            .explanation(explanationList)
            .extractedData(extractedData)
            .faceMatchConfidence(faceMatchConfidence)
            .isTampered(isTampered)
            .build();
}

    private int evaluateIdentityConsistency(Map<String, String> identityInfo) {
        int penalty = 0;
        String name = identityInfo.getOrDefault("name", "").trim();
        String idNumber = identityInfo.getOrDefault("idNumber", "").trim();
        String dob = identityInfo.getOrDefault("dob", "").trim();

        if (!name.isBlank() && name.matches(".*\\d.*")) {
            penalty += 15;
        }
        if (!name.isBlank() && name.split("\\s+").length > 4) {
            penalty += 10;
        }
        if (!idNumber.isBlank()) {
            String compactId = idNumber.replaceAll("[^A-Za-z0-9]", "");
            if (compactId.length() < 5) {
                penalty += 12;
            }
            if (compactId.length() > 24) {
                penalty += 8;
            }
            if (compactId.matches("^(.)\\1+$")) {
                penalty += 15;
            }
        }
        if (!dob.isBlank() && !dob.matches(".*\\d{2,4}.*")) {
            penalty += 10;
        }
        int age = deriveAgeFromDob(dob);
        if (age > 0 && (age < 14 || age > 110)) {
            penalty += 18;
        }
        return penalty;
    }

    private boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        return "application/pdf".equalsIgnoreCase(contentType)
                || (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));
    }

    private Map<String, Object> defaultFaceDetectionResult() {
        return Map.of(
                "faceCount", 0,
                "isFaceDetected", false,
                "highestConfidence", 0.0d,
                "faces", List.of()
        );
    }

    private Map<String, Object> defaultTamperDetectionResult() {
        return Map.of(
                "isTampered", false,
                "hasSuspiciousContent", false,
                "tamperScore", 0,
                "tamperSignals", List.of()
        );
    }

    private Map<String, Object> defaultQualityAnalysisResult() {
        return Map.of(
                "labels", List.of(),
                "qualityIndicators", Map.of(
                        "isBlurry", false,
                        "hasGoodLighting", true,
                        "isDocument", true
                )
        );
    }

    private String buildIdentitySignatureHash(Map<String, String> identityInfo) {
        String normalizedName = identityInfo.getOrDefault("name", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        String normalizedId = identityInfo.getOrDefault("idNumber", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        String normalizedDob = identityInfo.getOrDefault("dob", "")
                .trim()
                .replaceAll("\\s+", "");
        String signature = normalizedName + "|" + normalizedId + "|" + normalizedDob;
        return dataProtectionService.sha256Hex(signature.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String maskIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String compact = raw.replaceAll("\\s+", "");
        if (compact.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, compact.length() - 4)) + compact.substring(compact.length() - 4);
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        List<String> masked = new ArrayList<>();
        for (String part : parts) {
            if (part.length() <= 1) {
                masked.add(part);
            } else {
                masked.add(part.charAt(0) + "*".repeat(part.length() - 1));
            }
        }
        return String.join(" ", masked);
    }

    private String maskDob(String dob) {
        if (dob == null || dob.isBlank()) {
            return "";
        }
        String[] parts = dob.split("[/-]");
        if (parts.length == 3) {
            return "**/**/" + parts[2];
        }
        return "****";
    }

    private int deriveAgeFromDob(String dob) {
        if (dob == null || dob.isBlank()) {
            return -1;
        }
        String[] parts = dob.trim().split("[/-]");
        if (parts.length != 3) {
            return -1;
        }
        try {
            int p1 = Integer.parseInt(parts[0]);
            int p2 = Integer.parseInt(parts[1]);
            int p3 = Integer.parseInt(parts[2]);
            int year = p3 < 100 ? (2000 + p3) : p3;
            int day;
            int month;
            if (p1 > 12) {
                day = p1;
                month = p2;
            } else if (p2 > 12) {
                day = p2;
                month = p1;
            } else {
                day = p1;
                month = p2;
            }
            LocalDate dobDate = LocalDate.of(year, month, day);
            return Period.between(dobDate, LocalDate.now()).getYears();
        } catch (Exception ignored) {
            return -1;
        }
    }
}
