package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notification_email_digest_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_email_digest_item_source",
        columnNames = {"outbox_id", "source_event_key"}))
public class NotificationEmailDigestItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbox_id", nullable = false)
    private Long outboxId;

    @Column(name = "source_event_key", nullable = false, length = 255)
    private String sourceEventKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationEmailDigestItemJpaEntity() {}

    public NotificationEmailDigestItemJpaEntity(Long outboxId, String sourceEventKey, LocalDateTime createdAt) {
        this.outboxId = outboxId;
        this.sourceEventKey = sourceEventKey;
        this.createdAt = createdAt;
    }
}
