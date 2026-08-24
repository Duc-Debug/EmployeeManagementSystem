package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProviderAdapter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.expirationMs());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getIdValue())
                .claim("roleCode", user.getRole().getCode().getCode())
                .claim("tv", user.getTokenVersion())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getIssuedAt();
    }

    public Integer getTokenVersionFromToken(String token) {
        Claims claims = getClaims(token);
        Object tvObj = claims.get("tv");
        if (tvObj instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
