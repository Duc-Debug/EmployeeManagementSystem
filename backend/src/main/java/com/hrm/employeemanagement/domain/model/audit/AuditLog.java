package com.hrm.employeemanagement.domain.model.audit;

import java.time.LocalDateTime;

public class AuditLog {
    private Long id;
    private Long userId;
    private String action;
    private String tableName;
    private Long recordId;
    private LocalDateTime createdAt;

    public AuditLog(Long id, Long userId, String action, String tableName, Long recordId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.tableName = tableName;
        this.recordId = recordId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static AuditLog create(Long userId, String action, String tableName, Long recordId) {
        return new AuditLog(null, userId, action, tableName, recordId, LocalDateTime.now());
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
}
