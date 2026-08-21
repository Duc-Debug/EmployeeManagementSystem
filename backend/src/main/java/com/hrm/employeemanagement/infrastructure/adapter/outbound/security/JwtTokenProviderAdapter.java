package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider Adapter in Infrastructure Layer.
 * Enforces secure key management by requiring externalized secret key injection
 * and failing startup immediately if the secret is missing or insecure (< 256 bits).
 */
@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final int MINIMUM_SECRET_LENGTH = 32; // 256 bits for HMAC-SHA256
    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProviderAdapter(@Value("${jwt.secret}") String secret,
                                  @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT signing secret (jwt.secret) must be provided via environment variable or secret manager.");
        }
        if (secret.trim().length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT signing secret (jwt.secret) must be at least 256 bits (" + MINIMUM_SECRET_LENGTH + " characters) long for secure HMAC-SHA256 signing.");
        }
        this.key = Keys.hmacShaKeyFor(secret.trim().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getIdValue())
                .claim("roleCode", user.getRole().getCode().getCode())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
