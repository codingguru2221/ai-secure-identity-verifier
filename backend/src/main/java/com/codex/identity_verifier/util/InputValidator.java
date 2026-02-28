package com.codex.identity_verifier.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

public class InputValidator {

    // File validation constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/webp",
        "application/pdf"
    };
    
    // Security patterns
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$");

    /**
     * Validates a file upload
     * @param file The file to validate
     * @return ValidationResult containing validation result
     */
    public static ValidationResult validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ValidationResult.failure("File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return ValidationResult.failure("File type not specified");
        }

        boolean validType = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                validType = true;
                break;
            }
        }

        if (!validType) {
            return ValidationResult.failure("Invalid file type. Only JPEG, PNG, WEBP, and PDF files are allowed");
        }

        // Validate filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !FILENAME_PATTERN.matcher(originalFilename).matches()) {
            return ValidationResult.failure("Invalid filename characters detected");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a username
     * @param username The username to validate
     * @return ValidationResult containing validation result
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.failure("Username cannot be empty");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.failure("Username must be 3-20 characters long and contain only letters, numbers, and underscores");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a password
     * @param password The password to validate
     * @return ValidationResult containing validation result
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.failure("Password cannot be empty");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.failure("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number");
        }

        return ValidationResult.success();
    }

    /**
     * Sanitizes a string input by removing potentially dangerous characters
     * @param input The input to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        // Limit length to prevent abuse
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized.trim();
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}