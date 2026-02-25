package com.codex.identity_verifier.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "File size too large");
        response.put("message", "Maximum file size exceeded. Please upload a smaller file.");
        response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Missing parameter");
        response.put("message", "Required parameter '" + ex.getParameterName() + "' is missing");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal server error");
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        // Log the exception for debugging
        System.err.println("Error occurred at: " + request.getRequestURI());
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NoHandlerFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Endpoint not found");
        response.put("message", "The requested endpoint does not exist");
        response.put("status", HttpStatus.NOT_FOUND.value());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}