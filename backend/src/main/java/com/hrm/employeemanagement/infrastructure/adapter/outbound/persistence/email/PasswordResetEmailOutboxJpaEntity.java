package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_email_outbox")
public class PasswordResetEmailOutboxJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String username;

    @Column(name = "reset_token", nullable = false)
    private String resetToken;

    @Column(name = "validity_minutes", nullable = false)
    private Long validityMinutes;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetEmailOutboxJpaEntity() {
    }

    public PasswordResetEmailOutboxJpaEntity(String recipientEmail, String username,
                                              String resetToken, long validityMinutes) {
        this.recipientEmail = recipientEmail;
        this.username = username;
        this.resetToken = resetToken;
        this.validityMinutes = validityMinutes;
        this.attempts = 0;
        this.availableAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public String getRecipientEmail() { return recipientEmail; }
    public String getUsername() { return username; }
    public String getResetToken() { return resetToken; }
    public long getValidityMinutes() { return validityMinutes; }

    public void markDelivered(Instant now) {
        this.deliveredAt = now;
        this.resetToken = "DELIVERED";
        this.lastError = null;
    }

    public void scheduleRetry(Instant availableAt, String error) {
        this.attempts = attempts + 1;
        this.availableAt = availableAt;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    }
}
