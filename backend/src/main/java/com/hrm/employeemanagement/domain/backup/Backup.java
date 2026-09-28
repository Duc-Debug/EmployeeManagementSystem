package com.hrm.employeemanagement.domain.backup;

import com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException;

import java.time.LocalDateTime;

public class Backup {
    private Long id;
    private final String backupCode;
    private String title;
    private String description;
    private final BackupType backupType;
    private String fileName;
    private String filePath;
    private long fileSizeBytes;
    private String checksum;
    private BackupStatus status;
    private final boolean isAutomatic;
    private final Long createdBy;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public Backup(
            Long id,
            String backupCode,
            String title,
            String description,
            BackupType backupType,
            String fileName,
            String filePath,
            long fileSizeBytes,
            String checksum,
            BackupStatus status,
            boolean isAutomatic,
            Long createdBy,
            String createdByName,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            String errorMessage
    ) {
        this.id = id;
        this.backupCode = backupCode;
        this.title = title;
        this.description = description;
        this.backupType = backupType != null ? backupType : BackupType.FULL;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.status = status != null ? status : BackupStatus.IN_PROGRESS;
        this.isAutomatic = isAutomatic;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }

    public static Backup createNew(
            String backupCode,
            String title,
            String description,
            BackupType backupType,
            String fileName,
            String filePath,
            boolean isAutomatic,
            Long createdBy,
            String createdByName
    ) {
        return new Backup(
                null,
                backupCode,
                title,
                description,
                backupType,
                fileName,
                filePath,
                0L,
                null,
                BackupStatus.IN_PROGRESS,
                isAutomatic,
                createdBy,
                createdByName,
                LocalDateTime.now(),
                null,
                null
        );
    }

    public void markCompleted(long fileSizeBytes, String checksum) {
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.status = BackupStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = BackupStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void validateCanRestore() {
        if (this.status != BackupStatus.COMPLETED) {
            throw new InvalidBackupStatusException(
                    "Không thể phục hồi từ bản sao lưu này vì trạng thái không hợp lệ: " + this.status.getDescription()
            );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBackupCode() {
        return backupCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
