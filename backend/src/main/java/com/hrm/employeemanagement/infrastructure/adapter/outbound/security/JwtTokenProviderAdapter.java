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
import java.util.UUID;

/**
 * JWT Token Provider Adapter in Infrastructure Layer.
 * Enforces secure key management by requiring externalized secret key injection
 * and failing startup immediately if the secret is missing or insecure (< 256 bits).
 * Embeds unique JTI (JWT ID) and issuedAt claims in every generated token.
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
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
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

    @Override
    public long getRemainingExpirationMs(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object userIdClaim = claims.get("userId");
            if (userIdClaim instanceof Number number) {
                return number.longValue();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getJtiFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public long getIssuedAtTimestampFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date issuedAt = claims.getIssuedAt();
            return issuedAt != null ? issuedAt.getTime() : 0L;
        } catch (Exception ex) {
            return 0L;
        }
    }
}
