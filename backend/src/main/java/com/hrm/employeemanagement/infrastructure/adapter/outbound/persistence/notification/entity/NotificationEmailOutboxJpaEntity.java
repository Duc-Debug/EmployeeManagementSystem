package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_email_outbox")
public class NotificationEmailOutboxJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;
    @Column(name = "recipient_name")
    private String recipientName;
    @Column(nullable = false)
    private String subject;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    @Column(name = "digest_frequency", length = 30)
    private String digestFrequency;

    protected NotificationEmailOutboxJpaEntity() {}

    public NotificationEmailOutboxJpaEntity(Long recipientUserId, String recipientEmail,
            String recipientName, String subject, String body, LocalDateTime availableAt,
            LocalDateTime createdAt, String digestFrequency) {
        this.recipientUserId = recipientUserId;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.body = body;
        this.availableAt = availableAt;
        this.createdAt = createdAt;
        this.digestFrequency = digestFrequency;
    }

    public void append(String item) { this.body += System.lineSeparator() + item; }
    public void markDelivered(LocalDateTime when) { this.deliveredAt = when; }
    public Long getId() { return id; }
    public Long getRecipientUserId() { return recipientUserId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public String getDigestFrequency() { return digestFrequency; }
}
