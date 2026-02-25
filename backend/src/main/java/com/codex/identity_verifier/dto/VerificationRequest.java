package com.codex.identity_verifier.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VerificationRequest {
    private MultipartFile file;
}