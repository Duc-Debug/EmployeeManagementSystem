package com.hrm.employeemanagement.domain.user;

import com.hrm.employeemanagement.domain.exception.user.InvalidResetTokenException;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity for Password Reset Token.
 * Encapsulates token lifecycle and validity invariants.
 */
public class PasswordResetToken {
    private Long id;
    private UserId userId;
    private String tokenHash;
    private Instant expiryDate;
    private boolean used;
    private Instant createdAt;

    public PasswordResetToken(Long id, UserId userId, String tokenHash, Instant expiryDate, boolean used, Instant createdAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "UserId không được null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "TokenHash không được null");
        this.expiryDate = Objects.requireNonNull(expiryDate, "ExpiryDate không được null");
        this.used = used;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PasswordResetToken createNew(UserId userId, String tokenHash, long validityMinutes) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validityMinutes * 60);
        return new PasswordResetToken(null, userId, tokenHash, expiry, false, now);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiryDate);
    }

    public boolean isUsed() {
        return used;
    }

    public void validateAndMarkUsed(Instant now) {
        if (isUsed()) {
            throw new InvalidResetTokenException("Mã khôi phục mật khẩu này đã được sử dụng trước đó");
        }
        if (isExpired(now)) {
            throw new InvalidResetTokenException("Mã khôi phục mật khẩu này đã hết hạn. Vui lòng gửi yêu cầu mới");
        }
        this.used = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserId getUserId() {
        return userId;
    }

    public Long getUserIdValue() {
        return userId != null ? userId.value() : null;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
