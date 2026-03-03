package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;

@Service
public class LambdaFraudScoringService {

    private final LambdaClient lambdaClient;

    @Value("${aws.lambda.risk.enabled:false}")
    private boolean lambdaRiskEnabled;
    @Value("${aws.lambda.risk.function-name:}")
    private String functionName;

    public LambdaFraudScoringService(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    public int getAdditionalRiskScore(String fileHash, String identitySignatureHash, int tamperScore, boolean duplicateDetected) {
        if (!lambdaRiskEnabled || functionName == null || functionName.isBlank()) {
            return 0;
        }
        try {
            String payload = "{\"fileHash\":\"" + safe(fileHash) + "\","
                    + "\"identitySignatureHash\":\"" + safe(identitySignatureHash) + "\","
                    + "\"tamperScore\":" + tamperScore + ","
                    + "\"duplicateDetected\":" + duplicateDetected + "}";
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build();
            InvokeResponse response = lambdaClient.invoke(request);
            if (response.payload() == null) {
                return 0;
            }
            String body = response.payload().asUtf8String();
            return extractScore(body);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int extractScore(String body) {
        if (body == null) {
            return 0;
        }
        int idx = body.indexOf("\"score\"");
        if (idx < 0) {
            return 0;
        }
        int colon = body.indexOf(':', idx);
        if (colon < 0) {
            return 0;
        }
        int i = colon + 1;
        while (i < body.length() && (body.charAt(i) == ' ' || body.charAt(i) == '"')) {
            i++;
        }
        int start = i;
        while (i < body.length() && Character.isDigit(body.charAt(i))) {
            i++;
        }
        if (start == i) {
            return 0;
        }
        int score = Integer.parseInt(body.substring(start, i));
        return Math.max(0, Math.min(20, score));
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "");
    }
}
