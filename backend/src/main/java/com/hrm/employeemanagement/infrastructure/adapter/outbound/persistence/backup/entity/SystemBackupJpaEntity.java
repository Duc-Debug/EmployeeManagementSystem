package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.backup.entity;

import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_backups")
public class SystemBackupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_code", nullable = false, unique = true, length = 64)
    private String backupCode;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false, length = 32)
    private BackupType backupType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BackupStatus status;

    @Column(name = "is_automatic", nullable = false)
    private boolean isAutomatic;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public SystemBackupJpaEntity() {
    }

    public static SystemBackupJpaEntity fromDomain(Backup domain) {
        if (domain == null) return null;
        SystemBackupJpaEntity entity = new SystemBackupJpaEntity();
        entity.id = domain.getId();
        entity.backupCode = domain.getBackupCode();
        entity.title = domain.getTitle();
        entity.description = domain.getDescription();
        entity.backupType = domain.getBackupType();
        entity.fileName = domain.getFileName();
        entity.filePath = domain.getFilePath();
        entity.fileSizeBytes = domain.getFileSizeBytes();
        entity.checksum = domain.getChecksum();
        entity.status = domain.getStatus();
        entity.isAutomatic = domain.isAutomatic();
        entity.createdBy = domain.getCreatedBy();
        entity.createdByName = domain.getCreatedByName();
        entity.createdAt = domain.getCreatedAt();
        entity.completedAt = domain.getCompletedAt();
        entity.errorMessage = domain.getErrorMessage();
        return entity;
    }

    public Backup toDomain() {
        return new Backup(
                this.id,
                this.backupCode,
                this.title,
                this.description,
                this.backupType,
                this.fileName,
                this.filePath,
                this.fileSizeBytes,
                this.checksum,
                this.status,
                this.isAutomatic,
                this.createdBy,
                this.createdByName,
                this.createdAt,
                this.completedAt,
                this.errorMessage
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBackupCode() { return backupCode; }
    public void setBackupCode(String backupCode) { this.backupCode = backupCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BackupType getBackupType() { return backupType; }
    public void setBackupType(BackupType backupType) { this.backupType = backupType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }
    public boolean isAutomatic() { return isAutomatic; }
    public void setAutomatic(boolean automatic) { isAutomatic = automatic; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
