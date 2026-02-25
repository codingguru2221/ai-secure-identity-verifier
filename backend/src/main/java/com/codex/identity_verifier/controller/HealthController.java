package com.codex.identity_verifier.controller;

import com.codex.identity_verifier.dto.HealthCheckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("API Running 🚀")
                .build();
        return ResponseEntity.ok(response);
    }
}