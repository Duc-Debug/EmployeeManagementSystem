package com.hrm.employeemanagement.domain.audit;

import java.time.LocalDateTime;

public class AuditLog {
    private Long id;
    private Long userId;
    private String action;
    private String tableName;
    private Long recordId;
    private LocalDateTime createdAt;
    private final String oldValue;
    private final String newValue;

    public AuditLog(Long id, Long userId, String action, String tableName, Long recordId, LocalDateTime createdAt) {
        this(id, userId, action, tableName, recordId, createdAt, null, null);
    }

    public AuditLog(
            Long id,
            Long userId,
            String action,
            String tableName,
            Long recordId,
            LocalDateTime createdAt,
            String oldValue,
            String newValue
    ) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.tableName = tableName;
        this.recordId = recordId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static AuditLog create(Long userId, String action, String tableName, Long recordId) {
        return new AuditLog(null, userId, action, tableName, recordId, LocalDateTime.now());
    }

    public static AuditLog createChange(
            Long userId,
            String action,
            String tableName,
            Long recordId,
            String oldValue,
            String newValue
    ) {
        return new AuditLog(
                null,
                userId,
                action,
                tableName,
                recordId,
                LocalDateTime.now(),
                oldValue,
                newValue
        );
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getTableName() {
        return tableName;
    }

    public Long getRecordId() {
        return recordId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
