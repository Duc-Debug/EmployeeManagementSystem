package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity;

import com.hrm.employeemanagement.domain.backup.BackupAction;
import com.hrm.employeemanagement.domain.backup.BackupAuditLog;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_audit_logs")
public class BackupAuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    private BackupAction action;

    @Column(name = "backup_id")
    private Long backupId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BackupAuditLogJpaEntity() {
    }

    public static BackupAuditLogJpaEntity fromDomain(BackupAuditLog domain) {
        if (domain == null) return null;
        BackupAuditLogJpaEntity entity = new BackupAuditLogJpaEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.userEmail = domain.getUserEmail();
        entity.action = domain.getAction();
        entity.backupId = domain.getBackupId();
        entity.status = domain.getStatus();
        entity.reason = domain.getReason();
        entity.details = domain.getDetails();
        entity.ipAddress = domain.getIpAddress();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }

    public BackupAuditLog toDomain() {
        return new BackupAuditLog(
                this.id,
                this.userId,
                this.userEmail,
                this.action,
                this.backupId,
                this.status,
                this.reason,
                this.details,
                this.ipAddress,
                this.createdAt
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public BackupAction getAction() { return action; }
    public void setAction(BackupAction action) { this.action = action; }
    public Long getBackupId() { return backupId; }
    public void setBackupId(Long backupId) { this.backupId = backupId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
