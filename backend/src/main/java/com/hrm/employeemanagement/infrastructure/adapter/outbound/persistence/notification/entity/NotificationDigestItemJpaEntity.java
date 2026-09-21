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
@Table(name = "notification_digest_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_digest_item_event_source",
        columnNames = {"notification_event_id", "source_event_key"}))
public class NotificationDigestItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_event_id", nullable = false)
    private Long notificationEventId;

    @Column(name = "source_event_key", nullable = false, length = 255)
    private String sourceEventKey;

    @Column(name = "item_text", nullable = false, columnDefinition = "TEXT")
    private String itemText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationDigestItemJpaEntity() {}

    public NotificationDigestItemJpaEntity(
            Long notificationEventId, String sourceEventKey, String itemText, LocalDateTime createdAt) {
        this.notificationEventId = notificationEventId;
        this.sourceEventKey = sourceEventKey;
        this.itemText = itemText;
        this.createdAt = createdAt;
    }
}
