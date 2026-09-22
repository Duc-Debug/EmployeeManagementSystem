package com.hrm.employeemanagement.application.service.backup;

import com.hrm.employeemanagement.application.dto.backup.CreateBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.RestoreBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.UpdateBackupScheduleRequest;
import com.hrm.employeemanagement.application.port.outbound.*;
import com.hrm.employeemanagement.application.service.BackupService;
import com.hrm.employeemanagement.domain.backup.*;
import com.hrm.employeemanagement.domain.backup.exception.BackupNotFoundException;
import com.hrm.employeemanagement.domain.backup.exception.BackupRestoreFailedException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupRepositoryPort backupRepositoryPort;

    @Mock
    private BackupScheduleRepositoryPort backupScheduleRepositoryPort;

    @Mock
    private BackupAuditLogPort backupAuditLogPort;

    @Mock
    private BackupStoragePort backupStoragePort;

    @Mock
    private DatabaseBackupRestoreEnginePort backupRestoreEnginePort;

    private BackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new BackupService(
                backupRepositoryPort,
                backupScheduleRepositoryPort,
                backupAuditLogPort,
                backupStoragePort,
                backupRestoreEnginePort
        );
    }

    @Test
    @DisplayName("Tạo bản sao lưu theo yêu cầu thành công, tính toán kích thước, checksum và lưu audit log")
    void testCreateBackup_Success() throws Exception {
        CreateBackupRequest req = new CreateBackupRequest("Sao lưu Quý 3", "Mô tả kiểm thử", BackupType.FULL);
        Path mockPath = Paths.get("uploads/backups/test.json");

        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(mockPath);
        when(backupRepositoryPort.save(any(Backup.class))).thenAnswer(inv -> {
            Backup b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
        when(backupStoragePort.getFileSize(anyString())).thenReturn(1024L);
        when(backupStoragePort.calculateChecksum(anyString())).thenReturn("abc123sha256");

        Backup result = backupService.createBackup(req, 1L, "admin@company.com", "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Sao lưu Quý 3");
        assertThat(result.getStatus()).isEqualTo(BackupStatus.COMPLETED);
        assertThat(result.getFileSizeBytes()).isEqualTo(1024L);
        assertThat(result.getChecksum()).isEqualTo("abc123sha256");

        verify(backupRestoreEnginePort, times(1)).performBackup(any(File.class), eq(BackupType.FULL));
        verify(backupAuditLogPort, times(1)).save(any(BackupAuditLog.class));
    }

    @Test
    @DisplayName("Thực thi sao lưu tự động theo lịch định kỳ thành công")
    void testExecuteAutomaticBackup_Success() throws Exception {
        Path mockPath = Paths.get("uploads/backups/auto.json");
        BackupSchedule schedule = BackupSchedule.createDefault();
        schedule.setEnabled(true);

        when(backupScheduleRepositoryPort.findSchedule()).thenReturn(Optional.of(schedule));
        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(mockPath);
        when(backupRepositoryPort.save(any(Backup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(backupStoragePort.getFileSize(anyString())).thenReturn(2048L);
        when(backupStoragePort.calculateChecksum(anyString())).thenReturn("auto-sha256");

        Backup result = backupService.executeAutomaticBackup();

        assertThat(result).isNotNull();
        assertThat(result.isAutomatic()).isTrue();
        assertThat(result.getStatus()).isEqualTo(BackupStatus.COMPLETED);
        verify(backupScheduleRepositoryPort, times(1)).save(schedule);
    }

    @Test
    @DisplayName("Phục hồi dữ liệu thành công với mã xác nhận RESTORE và lý do hợp lệ")
    void testRestoreBackup_Success() throws Exception {
        Backup completedBackup = new Backup(
                50L,
                "BCK-20260922-001",
                "Bản sao lưu chuẩn",
                "Mô tả",
                BackupType.FULL,
                "bck.json",
                "uploads/backups/bck.json",
                4096L,
                "valid-checksum",
                BackupStatus.COMPLETED,
                false,
                1L,
                "admin",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );

        when(backupRepositoryPort.findById(50L)).thenReturn(Optional.of(completedBackup));
        when(backupStoragePort.exists(anyString())).thenReturn(true);
        when(backupStoragePort.calculateChecksum("uploads/backups/bck.json")).thenReturn("valid-checksum");
        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(Paths.get("uploads/backups/safety.json"));
        when(backupRepositoryPort.save(any(Backup.class))).thenAnswer(inv -> inv.getArgument(0));

        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Khắc phục lỗi dữ liệu phân bổ tuần 38");

        backupService.restoreBackup(50L, request, 1L, "admin@company.com", "127.0.0.1");

        verify(backupRestoreEnginePort, times(1)).performRestore(any(File.class), eq(BackupType.FULL));
        verify(backupAuditLogPort, atLeastOnce()).save(argThat(log ->
                log.getAction() == BackupAction.BACKUP_RESTORE && "SUCCESS".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("Chặn phục hồi khi mã xác nhận sai khác RESTORE")
    void testRestoreBackup_InvalidConfirmationCode_ThrowsException() {
        RestoreBackupRequest request = new RestoreBackupRequest("WRONG_CODE", "Khắc phục lỗi dữ liệu phân bổ");

        assertThatThrownBy(() -> backupService.restoreBackup(50L, request, 1L, "admin@company.com", "127.0.0.1"))
                .isInstanceOf(InvalidRestoreConfirmationException.class)
                .hasMessageContaining("RESTORE");

        verifyNoInteractions(backupRestoreEnginePort);
    }

    @Test
    @DisplayName("Chặn phục hồi khi lý do giải trình ngắn dưới 10 ký tự")
    void testRestoreBackup_ReasonTooShort_ThrowsException() {
        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Lỗi");

        assertThatThrownBy(() -> backupService.restoreBackup(50L, request, 1L, "admin@company.com", "127.0.0.1"))
                .isInstanceOf(InvalidRestoreConfirmationException.class)
                .hasMessageContaining("10 ký tự");

        verifyNoInteractions(backupRestoreEnginePort);
    }

    @Test
    @DisplayName("Chặn phục hồi khi bản sao lưu ở trạng thái FAILED hoặc IN_PROGRESS")
    void testRestoreBackup_FailedOrInProgressBackup_ThrowsException() {
        Backup failedBackup = new Backup(
                60L,
                "BCK-FAILED",
                "Bản sao lưu hỏng",
                null,
                BackupType.FULL,
                "failed.json",
                "uploads/backups/failed.json",
                0L,
                null,
                BackupStatus.FAILED,
                false,
                1L,
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Disk error"
        );

        when(backupRepositoryPort.findById(60L)).thenReturn(Optional.of(failedBackup));
        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Khắc phục lỗi dữ liệu phân bổ tuần 38");

        assertThatThrownBy(() -> backupService.restoreBackup(60L, request, 1L, "admin@company.com", "127.0.0.1"))
                .isInstanceOf(InvalidBackupStatusException.class)
                .hasMessageContaining("Thất bại");

        verifyNoInteractions(backupRestoreEnginePort);
    }

    @Test
    @DisplayName("Chặn phục hồi khi mã băm checksum của file không khớp với siêu dữ liệu")
    void testRestoreBackup_ChecksumMismatch_ThrowsException() {
        Backup tamperedBackup = new Backup(
                70L,
                "BCK-TAMPERED",
                "Bản sao lưu bị chỉnh sửa",
                null,
                BackupType.FULL,
                "tampered.json",
                "uploads/backups/tampered.json",
                2048L,
                "expected-checksum",
                BackupStatus.COMPLETED,
                false,
                1L,
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(backupRepositoryPort.findById(70L)).thenReturn(Optional.of(tamperedBackup));
        when(backupStoragePort.exists("uploads/backups/tampered.json")).thenReturn(true);
        when(backupStoragePort.calculateChecksum("uploads/backups/tampered.json")).thenReturn("tampered-checksum-mismatch");

        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Khắc phục lỗi dữ liệu phân bổ tuần 38");

        assertThatThrownBy(() -> backupService.restoreBackup(70L, request, 1L, "admin@company.com", "127.0.0.1"))
                .isInstanceOf(BackupRestoreFailedException.class)
                .hasMessageContaining("không khớp");

        verify(backupAuditLogPort, times(1)).save(argThat(log ->
                log.getAction() == BackupAction.BACKUP_RESTORE && "FAILED".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("Cập nhật cấu hình lịch sao lưu tự động thành công")
    void testUpdateSchedule_Success() {
        BackupSchedule existing = BackupSchedule.createDefault();
        when(backupScheduleRepositoryPort.findSchedule()).thenReturn(Optional.of(existing));
        when(backupScheduleRepositoryPort.save(any(BackupSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBackupScheduleRequest req = new UpdateBackupScheduleRequest();
        req.setEnabled(true);
        req.setFrequency(BackupFrequency.WEEKLY);
        req.setScheduledTime("03:30");
        req.setDayOfWeek("FRIDAY");
        req.setBackupType(BackupType.RESOURCE_PLAN);
        req.setRetentionDays(60);

        BackupSchedule updated = backupService.updateSchedule(req, 1L, "admin@company.com", "127.0.0.1");

        assertThat(updated.isEnabled()).isTrue();
        assertThat(updated.getFrequency()).isEqualTo(BackupFrequency.WEEKLY);
        assertThat(updated.getScheduledTime()).isEqualTo("03:30");
        assertThat(updated.getDayOfWeek()).isEqualTo("FRIDAY");
        assertThat(updated.getRetentionDays()).isEqualTo(60);
        assertThat(updated.getNextRunAt()).isNotNull();

        verify(backupAuditLogPort, times(1)).save(argThat(log ->
                log.getAction() == BackupAction.SCHEDULE_UPDATE && "SUCCESS".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("Xóa bản sao lưu thành công và xóa tệp vật lý")
    void testDeleteBackup_Success() {
        Backup b = new Backup(
                80L,
                "BCK-DEL",
                "Bản sao cần xóa",
                null,
                BackupType.FULL,
                "del.json",
                "uploads/backups/del.json",
                100L,
                "chk",
                BackupStatus.COMPLETED,
                false,
                1L,
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(backupRepositoryPort.findById(80L)).thenReturn(Optional.of(b));
        when(backupStoragePort.exists("uploads/backups/del.json")).thenReturn(true);

        backupService.deleteBackup(80L, "Dọn dẹp dung lượng", 1L, "admin@company.com", "127.0.0.1");

        verify(backupStoragePort, times(1)).deleteBackupFile("uploads/backups/del.json");
        verify(backupRepositoryPort, times(1)).deleteById(80L);
        verify(backupAuditLogPort, times(1)).save(argThat(log ->
                log.getAction() == BackupAction.BACKUP_DELETE && "SUCCESS".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("Tải lên bản sao lưu thành công, tính toán kích thước, checksum và tạo bản ghi hoàn tất")
    void testUploadBackup_Success() {
        byte[] content = "{\"tables\": {\"users\": []}}".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(content);
        Path mockPath = Paths.get("uploads/backups/upload_test.json");

        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(mockPath);
        when(backupStoragePort.readBackupFile(anyString())).thenAnswer(inv -> new ByteArrayInputStream(content));
        when(backupStoragePort.getFileSize(anyString())).thenReturn((long) content.length);
        when(backupStoragePort.calculateChecksum(anyString())).thenReturn("upload-sha256");
        when(backupRepositoryPort.save(any(Backup.class))).thenAnswer(inv -> inv.getArgument(0));

        Backup uploaded = backupService.uploadBackup(
                "custom_backup.json",
                "Bản tải lên từ server cũ",
                "Mô tả",
                BackupType.FULL,
                is,
                content.length,
                1L,
                "admin@company.com",
                "127.0.0.1"
        );

        assertThat(uploaded).isNotNull();
        assertThat(uploaded.getStatus()).isEqualTo(BackupStatus.COMPLETED);
        assertThat(uploaded.getFileSizeBytes()).isEqualTo(content.length);
        assertThat(uploaded.getChecksum()).isEqualTo("upload-sha256");
        verify(backupStoragePort, times(1)).storeBackupFile(anyString(), any());
        verify(backupAuditLogPort, times(1)).save(argThat(log ->
                log.getAction() == BackupAction.BACKUP_UPLOAD && "SUCCESS".equals(log.getStatus())
        ));
    }

    @Test
    @DisplayName("Tải lên tệp không phải .json ném lỗi IllegalArgumentException")
    void testUploadBackup_NonJsonExtension_ThrowsException() {
        byte[] content = "SELECT * FROM users;".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(content);

        assertThatThrownBy(() -> backupService.uploadBackup(
                "dump.sql",
                "SQL Dump",
                "Test",
                BackupType.FULL,
                is,
                content.length,
                1L,
                "admin@company.com",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".json");
    }

    @Test
    @DisplayName("Tải lên tệp JSON không hợp lệ hoặc thiếu dữ liệu bảng ném lỗi IllegalArgumentException")
    void testUploadBackup_InvalidJsonContent_ThrowsException() {
        byte[] content = "{\"corrupted_json\": true}".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(content);
        Path mockPath = Paths.get("uploads/backups/upload_test.json");

        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(mockPath);
        when(backupStoragePort.readBackupFile(anyString())).thenAnswer(inv -> new ByteArrayInputStream(content));

        assertThatThrownBy(() -> backupService.uploadBackup(
                "corrupted.json",
                "Corrupted JSON",
                "Test",
                BackupType.FULL,
                is,
                content.length,
                1L,
                "admin@company.com",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tables");

        verify(backupStoragePort, times(1)).deleteBackupFile(anyString());
    }

    @Test
    @DisplayName("Khi snapshot an toàn (Safety snapshot) thất bại thì hủy toàn bộ tiến trình phục hồi")
    void testRestoreBackup_SafetySnapshotFailure_ThrowsExceptionAndAbortsRestore() throws Exception {
        Backup validBackup = new Backup(
                90L,
                "BCK-VALID",
                "Bản sao hợp lệ",
                null,
                BackupType.FULL,
                "valid.json",
                "uploads/backups/valid.json",
                1024L,
                "valid-chk",
                BackupStatus.COMPLETED,
                false,
                1L,
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(backupRepositoryPort.findById(90L)).thenReturn(Optional.of(validBackup));
        when(backupStoragePort.exists("uploads/backups/valid.json")).thenReturn(true);
        when(backupStoragePort.calculateChecksum("uploads/backups/valid.json")).thenReturn("valid-chk");
        when(backupStoragePort.resolveBackupPath(anyString())).thenReturn(Paths.get("uploads/backups/safety.json"));
        when(backupRepositoryPort.save(any(Backup.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Disk full")).when(backupRestoreEnginePort).performBackup(any(), any());

        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Phục hồi kế hoạch dự án tuần 38");

        assertThatThrownBy(() -> backupService.restoreBackup(90L, request, 1L, "admin@company.com", "127.0.0.1"))
                .isInstanceOf(BackupRestoreFailedException.class)
                .hasMessageContaining("Safety snapshot");

        verify(backupRestoreEnginePort, never()).performRestore(any(), any());
    }

    @Test
    @DisplayName("Thực thi chính sách retention xóa các bản sao lưu đã hết hạn lưu trữ")
    void testApplyRetentionPolicy_DeletesExpiredBackups() {
        Backup expiredBackup = new Backup(
                95L,
                "BCK-EXPIRED",
                "Bản sao hết hạn",
                null,
                BackupType.FULL,
                "expired.json",
                "uploads/backups/expired.json",
                1024L,
                "chk",
                BackupStatus.COMPLETED,
                true,
                1L,
                "admin",
                LocalDateTime.now().minusDays(35),
                LocalDateTime.now().minusDays(35),
                null
        );

        when(backupRepositoryPort.findExpiredBackups(any())).thenReturn(List.of(expiredBackup));
        when(backupStoragePort.exists("uploads/backups/expired.json")).thenReturn(true);

        backupService.applyRetentionPolicy(30);

        verify(backupStoragePort, times(1)).deleteBackupFile("uploads/backups/expired.json");
        verify(backupRepositoryPort, times(1)).deleteById(95L);
    }
}
