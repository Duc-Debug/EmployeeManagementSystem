package com.hrm.employeemanagement.application.dto.backup;

import com.hrm.employeemanagement.domain.backup.BackupAction;
import com.hrm.employeemanagement.domain.backup.BackupAuditLog;

import java.time.LocalDateTime;

public class BackupAuditLogResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private BackupAction action;
    private String actionDescription;
    private Long backupId;
    private String status;
    private String reason;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static BackupAuditLogResponse fromDomain(BackupAuditLog log) {
        if (log == null) return null;
        BackupAuditLogResponse res = new BackupAuditLogResponse();
        res.id = log.getId();
        res.userId = log.getUserId();
        res.userEmail = log.getUserEmail();
        res.action = log.getAction();
        res.actionDescription = log.getAction() != null ? log.getAction().getDescription() : "";
        res.backupId = log.getBackupId();
        res.status = log.getStatus();
        res.reason = log.getReason();
        res.details = log.getDetails();
        res.ipAddress = log.getIpAddress();
        res.createdAt = log.getCreatedAt();
        return res;
    }

    public Long getId() {
        return id;
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

    public String getActionDescription() {
        return actionDescription;
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
