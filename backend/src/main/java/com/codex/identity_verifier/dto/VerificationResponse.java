package com.codex.identity_verifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationResponse {
    private String riskLevel;
    private Integer riskScore;
    private List<String> explanation;
    private ExtractedData extractedData;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExtractedData {
        private String name;
        private String idNumber;
        private String dob;
    }
}