package com.hrm.employeemanagement.application.service;

import com.hrm.employeemanagement.application.dto.backup.CreateBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.RestoreBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.UpdateBackupScheduleRequest;
import com.hrm.employeemanagement.application.port.inbound.*;
import com.hrm.employeemanagement.application.port.outbound.*;
import com.hrm.employeemanagement.domain.backup.*;
import com.hrm.employeemanagement.domain.backup.exception.BackupNotFoundException;
import com.hrm.employeemanagement.domain.backup.exception.BackupRestoreFailedException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BackupService implements
        CreateBackupUseCase,
        RestoreBackupUseCase,
        GetBackupsUseCase,
        DownloadBackupUseCase,
        DeleteBackupUseCase,
        UpdateBackupScheduleUseCase,
        GetBackupScheduleUseCase,
        GetBackupAuditLogsUseCase,
        UploadBackupUseCase {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupRepositoryPort backupRepositoryPort;
    private final BackupScheduleRepositoryPort backupScheduleRepositoryPort;
    private final BackupAuditLogPort backupAuditLogPort;
    private final BackupStoragePort backupStoragePort;
    private final DatabaseBackupRestoreEnginePort backupRestoreEnginePort;

    public BackupService(
            BackupRepositoryPort backupRepositoryPort,
            BackupScheduleRepositoryPort backupScheduleRepositoryPort,
            BackupAuditLogPort backupAuditLogPort,
            BackupStoragePort backupStoragePort,
            DatabaseBackupRestoreEnginePort backupRestoreEnginePort
    ) {
        this.backupRepositoryPort = backupRepositoryPort;
        this.backupScheduleRepositoryPort = backupScheduleRepositoryPort;
        this.backupAuditLogPort = backupAuditLogPort;
        this.backupStoragePort = backupStoragePort;
        this.backupRestoreEnginePort = backupRestoreEnginePort;
    }

    @Override
    public Backup createBackup(CreateBackupRequest request, Long currentUserId, String currentUserEmail, String clientIp) {
        String timestamp = LocalDateTime.now().format(CODE_DATE_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        String backupCode = "BCK-" + timestamp + "-" + randomSuffix;
        String fileName = backupCode + ".json";
        Path resolvedPath = backupStoragePort.resolveBackupPath(fileName);

        String title = (request != null && request.getTitle() != null && !request.getTitle().trim().isEmpty())
                ? request.getTitle().trim()
                : "Bản sao lưu thủ công " + timestamp;
        String description = (request != null) ? request.getDescription() : null;
        BackupType backupType = (request != null && request.getBackupType() != null) ? request.getBackupType() : BackupType.FULL;

        Backup backup = Backup.createNew(
                backupCode,
                title,
                description,
                backupType,
                fileName,
                resolvedPath.toString(),
                false,
                currentUserId,
                currentUserEmail
        );
        backup = backupRepositoryPort.save(backup);

        try {
            File targetFile = resolvedPath.toFile();
            backupRestoreEnginePort.performBackup(targetFile, backupType);

            long fileSizeBytes = backupStoragePort.getFileSize(resolvedPath.toString());
            String checksum = backupStoragePort.calculateChecksum(resolvedPath.toString());

            backup.markCompleted(fileSizeBytes, checksum);
            backup = backupRepositoryPort.save(backup);

            backupAuditLogPort.save(BackupAuditLog.create(
                    currentUserId,
                    currentUserEmail,
                    BackupAction.BACKUP_CREATE,
                    backup.getId(),
                    "SUCCESS",
                    "Tạo bản sao lưu thành công",
                    "Dung lượng: " + fileSizeBytes + " bytes, Checksum: " + checksum,
                    clientIp
            ));

            return backup;
        } catch (Exception e) {
            log.error("Lỗi khi tạo bản sao lưu {}: {}", backupCode, e.getMessage(), e);
            backup.markFailed(e.getMessage());
            backupRepositoryPort.save(backup);

            backupAuditLogPort.save(BackupAuditLog.create(
                    currentUserId,
                    currentUserEmail,
                    BackupAction.BACKUP_CREATE,
                    backup.getId(),
                    "FAILED",
                    "Tạo bản sao lưu thất bại: " + e.getMessage(),
                    null,
                    clientIp
            ));

            throw new RuntimeException("Tạo bản sao lưu thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public Backup executeAutomaticBackup() {
        String timestamp = LocalDateTime.now().format(CODE_DATE_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        String backupCode = "BCK-AUTO-" + timestamp + "-" + randomSuffix;
        String fileName = backupCode + ".json";
        Path resolvedPath = backupStoragePort.resolveBackupPath(fileName);

        BackupSchedule schedule = getSchedule();
        BackupType backupType = schedule.getBackupType() != null ? schedule.getBackupType() : BackupType.FULL;

        Backup backup = Backup.createNew(
                backupCode,
                "Bản sao lưu tự động định kỳ (" + schedule.getFrequency().getLabel() + ")",
                "Được tạo tự động theo cấu hình lịch hệ thống lúc " + schedule.getScheduledTime(),
                backupType,
                fileName,
                resolvedPath.toString(),
                true,
                null,
                "Hệ thống (Auto Scheduler)"
        );
        backup = backupRepositoryPort.save(backup);

        try {
            File targetFile = resolvedPath.toFile();
            backupRestoreEnginePort.performBackup(targetFile, backupType);

            long fileSizeBytes = backupStoragePort.getFileSize(resolvedPath.toString());
            String checksum = backupStoragePort.calculateChecksum(resolvedPath.toString());

            backup.markCompleted(fileSizeBytes, checksum);
            backup = backupRepositoryPort.save(backup);

            schedule.recordRunSuccess(LocalDateTime.now());
            backupScheduleRepositoryPort.save(schedule);

            backupAuditLogPort.save(BackupAuditLog.create(
                    null,
                    "system@scheduler",
                    BackupAction.BACKUP_CREATE,
                    backup.getId(),
                    "SUCCESS",
                    "Sao lưu tự động định kỳ thành công",
                    "Dung lượng: " + fileSizeBytes + " bytes, Checksum: " + checksum,
                    "127.0.0.1"
            ));

            return backup;
        } catch (Exception e) {
            log.error("Lỗi khi chạy sao lưu tự động {}: {}", backupCode, e.getMessage(), e);
            backup.markFailed(e.getMessage());
            backupRepositoryPort.save(backup);

            backupAuditLogPort.save(BackupAuditLog.create(
                    null,
                    "system@scheduler",
                    BackupAction.BACKUP_CREATE,
                    backup.getId(),
                    "FAILED",
                    "Sao lưu tự động thất bại: " + e.getMessage(),
                    null,
                    "127.0.0.1"
            ));

            return backup;
        }
    }

    @Override
    public void restoreBackup(Long backupId, RestoreBackupRequest request, Long currentUserId, String currentUserEmail, String clientIp) {
        if (request == null || !"RESTORE".equalsIgnoreCase(request.getConfirmationCode())) {
            throw new InvalidRestoreConfirmationException("Mã xác nhận không chính xác. Vui lòng nhập đúng 'RESTORE' để tiến hành phục hồi dữ liệu.");
        }
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            throw new InvalidRestoreConfirmationException("Vui lòng cung cấp lý do giải trình phục hồi tối thiểu 10 ký tự.");
        }

        Backup backup = backupRepositoryPort.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        // Kiểm tra tính hợp lệ của trạng thái
        backup.validateCanRestore();

        // Kiểm tra file vật lý tồn tại
        if (!backupStoragePort.exists(backup.getFilePath())) {
            throw new BackupRestoreFailedException("Tệp sao lưu không tồn tại trên hệ thống lưu trữ: " + backup.getFileName());
        }

        // Kiểm tra mã băm checksum tính toàn vẹn
        String currentChecksum = backupStoragePort.calculateChecksum(backup.getFilePath());
        if (backup.getChecksum() != null && !backup.getChecksum().equalsIgnoreCase(currentChecksum)) {
            backupAuditLogPort.save(BackupAuditLog.create(
                    currentUserId,
                    currentUserEmail,
                    BackupAction.BACKUP_RESTORE,
                    backupId,
                    "FAILED",
                    "Chặn phục hồi: Mã băm Checksum không khớp",
                    "Expected: " + backup.getChecksum() + ", Actual: " + currentChecksum,
                    clientIp
            ));
            throw new BackupRestoreFailedException("Tệp sao lưu đã bị thay đổi hoặc bị hỏng (Mã băm SHA-256 không khớp).");
        }

        // Tự động tạo điểm an toàn (Safety snapshot) trước khi phục hồi
        try {
            String safetyCode = "SAFETY-PRE-RESTORE-" + LocalDateTime.now().format(CODE_DATE_FORMAT);
            Path safetyPath = backupStoragePort.resolveBackupPath(safetyCode + ".json");
            Backup safetyBackup = Backup.createNew(
                    safetyCode,
                    "Điểm an toàn tự động trước khi phục hồi",
                    "Tự động tạo trước khi phục hồi bản sao lưu: " + backup.getBackupCode(),
                    BackupType.FULL,
                    safetyCode + ".json",
                    safetyPath.toString(),
                    true,
                    currentUserId,
                    currentUserEmail
            );
            safetyBackup = backupRepositoryPort.save(safetyBackup);
            backupRestoreEnginePort.performBackup(safetyPath.toFile(), BackupType.FULL);
            safetyBackup.markCompleted(backupStoragePort.getFileSize(safetyPath.toString()), backupStoragePort.calculateChecksum(safetyPath.toString()));
            backupRepositoryPort.save(safetyBackup);
        } catch (Exception e) {
            log.warn("Không thể tạo bản snapshot an toàn trước khi phục hồi: {}", e.getMessage());
        }

        // Thực hiện phục hồi
        try {
            File backupFile = new File(backup.getFilePath());
            backupRestoreEnginePort.performRestore(backupFile, backup.getBackupType());

            backupAuditLogPort.save(BackupAuditLog.create(
                    currentUserId,
                    currentUserEmail,
                    BackupAction.BACKUP_RESTORE,
                    backup.getId(),
                    "SUCCESS",
                    "Phục hồi dữ liệu thành công. Lý do: " + request.getReason().trim(),
                    "Bản sao lưu: " + backup.getBackupCode() + ", Loại: " + backup.getBackupType(),
                    clientIp
            ));
        } catch (Exception e) {
            log.error("Phục hồi dữ liệu thất bại cho bản sao lưu {}: {}", backup.getBackupCode(), e.getMessage(), e);
            backupAuditLogPort.save(BackupAuditLog.create(
                    currentUserId,
                    currentUserEmail,
                    BackupAction.BACKUP_RESTORE,
                    backup.getId(),
                    "FAILED",
                    "Phục hồi dữ liệu thất bại: " + e.getMessage(),
                    "Lý do yêu cầu: " + request.getReason(),
                    clientIp
            ));
            throw new BackupRestoreFailedException("Quá trình phục hồi dữ liệu gặp lỗi: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Backup> getBackups(BackupType type, BackupStatus status, String search) {
        return backupRepositoryPort.findFiltered(type, status, search);
    }

    @Override
    public Backup getBackupById(Long id) {
        return backupRepositoryPort.findById(id)
                .orElseThrow(() -> new BackupNotFoundException(id));
    }

    @Override
    public InputStream downloadBackup(Long backupId, Long currentUserId, String currentUserEmail, String clientIp) {
        Backup backup = backupRepositoryPort.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        if (!backupStoragePort.exists(backup.getFilePath())) {
            throw new BackupNotFoundException("Tệp sao lưu không tồn tại trên máy chủ: " + backup.getFileName());
        }

        backupAuditLogPort.save(BackupAuditLog.create(
                currentUserId,
                currentUserEmail,
                BackupAction.BACKUP_DOWNLOAD,
                backup.getId(),
                "SUCCESS",
                "Tải về bản sao lưu: " + backup.getBackupCode(),
                "File: " + backup.getFileName(),
                clientIp
        ));

        return backupStoragePort.readBackupFile(backup.getFilePath());
    }

    @Override
    public Backup getBackupForDownload(Long backupId) {
        return backupRepositoryPort.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));
    }

    @Override
    public void deleteBackup(Long backupId, String reason, Long currentUserId, String currentUserEmail, String clientIp) {
        Backup backup = backupRepositoryPort.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        if (backupStoragePort.exists(backup.getFilePath())) {
            backupStoragePort.deleteBackupFile(backup.getFilePath());
        }

        backupRepositoryPort.deleteById(backupId);

        backupAuditLogPort.save(BackupAuditLog.create(
                currentUserId,
                currentUserEmail,
                BackupAction.BACKUP_DELETE,
                backupId,
                "SUCCESS",
                "Xóa bản sao lưu. Lý do: " + (reason != null ? reason : "Không có lý do"),
                "Mã bản sao: " + backup.getBackupCode(),
                clientIp
        ));
    }

    @Override
    public BackupSchedule updateSchedule(UpdateBackupScheduleRequest request, Long currentUserId, String currentUserEmail, String clientIp) {
        BackupSchedule schedule = getSchedule();
        schedule.update(
                request.isEnabled(),
                request.getFrequency(),
                request.getScheduledTime(),
                request.getDayOfWeek(),
                request.getBackupType(),
                request.getRetentionDays(),
                currentUserId
        );
        schedule = backupScheduleRepositoryPort.save(schedule);

        backupAuditLogPort.save(BackupAuditLog.create(
                currentUserId,
                currentUserEmail,
                BackupAction.SCHEDULE_UPDATE,
                null,
                "SUCCESS",
                "Cập nhật cấu hình lịch sao lưu tự động",
                "Bật: " + schedule.isEnabled() + ", Tần suất: " + schedule.getFrequency() + ", Giờ: " + schedule.getScheduledTime(),
                clientIp
        ));

        return schedule;
    }

    @Override
    public BackupSchedule getSchedule() {
        return backupScheduleRepositoryPort.findSchedule()
                .orElseGet(() -> backupScheduleRepositoryPort.save(BackupSchedule.createDefault()));
    }

    @Override
    public List<BackupAuditLog> getAuditLogs() {
        return backupAuditLogPort.findRecent(100);
    }

    @Override
    public Backup uploadBackup(
            String originalFileName,
            String title,
            String description,
            BackupType backupType,
            InputStream inputStream,
            long fileSizeBytes,
            Long currentUserId,
            String currentUserEmail,
            String clientIp
    ) {
        String timestamp = LocalDateTime.now().format(CODE_DATE_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        String backupCode = "BCK-UPLOAD-" + timestamp + "-" + randomSuffix;
        String fileName = backupCode + "_" + (originalFileName != null ? originalFileName : "upload.json");
        Path resolvedPath = backupStoragePort.resolveBackupPath(fileName);

        backupStoragePort.storeBackupFile(fileName, inputStream);
        long actualSize = backupStoragePort.getFileSize(resolvedPath.toString());
        String checksum = backupStoragePort.calculateChecksum(resolvedPath.toString());

        String uploadTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : "Bản sao lưu tải lên " + timestamp;
        Backup backup = new Backup(
                null,
                backupCode,
                uploadTitle,
                description,
                backupType != null ? backupType : BackupType.FULL,
                fileName,
                resolvedPath.toString(),
                actualSize > 0 ? actualSize : fileSizeBytes,
                checksum,
                BackupStatus.COMPLETED,
                false,
                currentUserId,
                currentUserEmail,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        backup = backupRepositoryPort.save(backup);

        backupAuditLogPort.save(BackupAuditLog.create(
                currentUserId,
                currentUserEmail,
                BackupAction.BACKUP_UPLOAD,
                backup.getId(),
                "SUCCESS",
                "Tải lên bản sao lưu thành công",
                "Tệp gốc: " + originalFileName + ", Dung lượng: " + actualSize + " bytes",
                clientIp
        ));

        return backup;
    }

    public void recordAccessDenied(Long userId, String userEmail, String action, String reason, String clientIp) {
        backupAuditLogPort.save(BackupAuditLog.create(
                userId,
                userEmail,
                BackupAction.ACCESS_DENIED,
                null,
                "FORBIDDEN",
                "Từ chối truy cập: " + reason,
                "Hành động yêu cầu: " + action,
                clientIp
        ));
    }
}
