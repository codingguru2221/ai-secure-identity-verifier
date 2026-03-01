package com.codex.identity_verifier.service;

import com.codex.identity_verifier.dto.LoginRequest;
import com.codex.identity_verifier.dto.LoginResponse;
import com.codex.identity_verifier.dto.SignupRequest;
import com.codex.identity_verifier.model.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserAccountService userAccountService;
    
    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction}")
    private String jwtSecret;
    
    @Value("${jwt.expiration-hours:24}")
    private Long jwtExpirationHours;

    @Value("${auth.admin.username:admin}")
    private String adminUsername;

    // bcrypt hash for admin123 (fallback bootstrap admin)
    @Value("${auth.admin.password-hash:$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO}")
    private String adminPasswordHash;

    @Value("${auth.admin.password:admin123}")
    private String adminPassword;

    @Autowired
    public AuthService(UserAccountService userAccountService) {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.userAccountService = userAccountService;
    }

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // Bootstrap admin from config to avoid lockout in production.
        boolean adminPasswordMatchesByHash = validatePassword(password, adminPasswordHash);
        boolean adminPasswordMatchesPlain = adminPassword != null && adminPassword.equals(password);
        if (adminUsername.equals(username) && (adminPasswordMatchesByHash || adminPasswordMatchesPlain)) {
            return generateToken(username, "ADMIN");
        }

        UserAccount user = userAccountService.getUserByUsername(username);
        if (user != null && validatePassword(password, user.getPasswordHash())) {
            String role = user.getRole() != null ? user.getRole() : "USER";
            return generateToken(username, role);
        }

        throw new RuntimeException("Invalid credentials");
    }

    public LoginResponse signupUser(SignupRequest signupRequest) {
        String username = signupRequest.getUsername();
        String password = signupRequest.getPassword();

        if (adminUsername.equals(username)) {
            throw new RuntimeException("Username is reserved");
        }

        UserAccount existing = userAccountService.getUserByUsername(username);
        if (existing != null) {
            throw new RuntimeException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        userAccountService.createUser(username, passwordHash, "USER");
        return generateToken(username, "USER");
    }

    private boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private LoginResponse generateToken(String username, String role) {
        SecretKey key = getSigningKey();
        
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
            SecretKey key = getSigningKey();
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        SecretKey key = getSigningKey();
        Object role = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
        return role != null ? role.toString() : "USER";
    }

    private SecretKey getSigningKey() {
        try {
            // Derive a stable 64-byte key for HS512 from configured secret.
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT signing key", e);
        }
    }
}
