package com.hrm.employeemanagement.domain.notification.dedup;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Entity lưu vết lịch sử thay đổi cấu hình chống gửi trùng (TC-04).
 */
public class NotificationDedupConfigHistory {

    private Long id;
    private final Long actorUserId;
    private final String action;
    private final String previousValue;
    private final String newValue;
    private final String changeSummary;
    private final LocalDateTime createdAt;

    public NotificationDedupConfigHistory(
            Long id,
            Long actorUserId,
            String action,
            String previousValue,
            String newValue,
            String changeSummary,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.previousValue = previousValue;
        this.newValue = Objects.requireNonNull(newValue, "newValue must not be null");
        this.changeSummary = Objects.requireNonNull(changeSummary, "changeSummary must not be null");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static NotificationDedupConfigHistory create(
            Long actorUserId,
            String action,
            String previousValue,
            String newValue,
            String changeSummary
    ) {
        return new NotificationDedupConfigHistory(
                null,
                actorUserId,
                action,
                previousValue,
                newValue,
                changeSummary,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
