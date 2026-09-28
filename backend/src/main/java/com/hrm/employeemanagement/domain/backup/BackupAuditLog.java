package com.hrm.employeemanagement.domain.backup;

import java.time.LocalDateTime;

public class BackupAuditLog {
    private Long id;
    private Long userId;
    private String userEmail;
    private BackupAction action;
    private Long backupId;
    private String status; // "SUCCESS", "FAILED", "FORBIDDEN"
    private String reason;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public BackupAuditLog(
            Long id,
            Long userId,
            String userEmail,
            BackupAction action,
            Long backupId,
            String status,
            String reason,
            String details,
            String ipAddress,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.backupId = backupId;
        this.status = status != null ? status : "SUCCESS";
        this.reason = reason;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static BackupAuditLog create(
            Long userId,
            String userEmail,
            BackupAction action,
            Long backupId,
            String status,
            String reason,
            String details,
            String ipAddress
    ) {
        return new BackupAuditLog(
                null,
                userId,
                userEmail,
                action,
                backupId,
                status,
                reason,
                details,
                ipAddress,
                LocalDateTime.now()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public BackupAction getAction() {
        return action;
    }

    public Long getBackupId() {
        return backupId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
