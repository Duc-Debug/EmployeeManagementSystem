package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Domain Entity ghi lại vết kiểm toán của Thành viên trên Trung tâm thông báo (TC-03).
 */
public class NotificationAuditLog {
    private final Long id;
    private final UserId actorUserId;
    private final NotificationAuditAction action;
    private final String targetType;
    private final Long targetId; // Có thể null khi action là MARK_ALL_READ (targetType = NOTIFICATION_CENTER)
    private final String detail;
    private final LocalDateTime createdAt;

    public NotificationAuditLog(
            Long id,
            UserId actorUserId,
            NotificationAuditAction action,
            String targetType,
            Long targetId,
            String detail,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId không được null");
        this.action = Objects.requireNonNull(action, "action không được null");
        this.targetType = Objects.requireNonNull(targetType, "targetType không được null");
        this.targetId = targetId;
        this.detail = detail;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static NotificationAuditLog create(
            UserId actorUserId,
            NotificationAuditAction action,
            String targetType,
            Long targetId,
            String detail
    ) {
        return new NotificationAuditLog(
                null,
                actorUserId,
                action,
                targetType,
                targetId,
                detail,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public UserId getActorUserId() {
        return actorUserId;
    }

    public NotificationAuditAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
