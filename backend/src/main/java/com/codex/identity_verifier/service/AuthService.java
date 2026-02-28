package com.codex.identity_verifier.service;

import com.codex.identity_verifier.dto.LoginRequest;
import com.codex.identity_verifier.dto.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction}")
    private String jwtSecret;
    
    @Value("${jwt.expiration-hours:24}")
    private Long jwtExpirationHours;

    public AuthService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        // In a real implementation, you would validate against a database
        // For demo purposes, we'll use hardcoded credentials
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        
        // Demo credentials
        if ("admin".equals(username) && validatePassword(password, "$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO")) {
            // admin password: admin123
            return generateToken(username, "ADMIN");
        } else if ("user".equals(username) && validatePassword(password, "$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO")) {
            // user password: user123
            return generateToken(username, "USER");
        }
        
        throw new RuntimeException("Invalid credentials");
    }

    private boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private LoginResponse generateToken(String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpirationHours * 60 * 60 * 1000));
        
        String token = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        
        return LoginResponse.builder()
                .token(token)
                .username(username)
                .role(role)
                .expiresIn(expiryDate.getTime())
                .build();
    }

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}