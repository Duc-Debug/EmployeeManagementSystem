package com.hrm.employeemanagement.application.service.notification.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.UpdateNotificationDedupConfigCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfigHistory;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class NotificationDedupConfigServiceTest {

    private NotificationDedupConfigRepositoryPort configRepositoryPort;
    private GetAuthenticatedUserPort authenticatedUserPort;
    private PermissionQueryPort permissionQueryPort;
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase;
    private NotificationDedupConfigService service;

    private User adminUser;
    private User nonAdminUser;

    @BeforeEach
    void setUp() {
        configRepositoryPort = mock(NotificationDedupConfigRepositoryPort.class);
        authenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        permissionQueryPort = mock(PermissionQueryPort.class);
        deniedAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);
        scanOverloadAndAlertUseCase = mock(ScanOverloadAndAlertUseCase.class);

        service = new NotificationDedupConfigService(
                configRepositoryPort,
                authenticatedUserPort,
                permissionQueryPort,
                deniedAuditLogPort,
                scanOverloadAndAlertUseCase
        );

        adminUser = new User(
                new UserId(1L),
                "admin",
                "hash",
                new Role(new RoleId(6L), com.hrm.employeemanagement.domain.role.RoleCode.VT_06, "Quản trị viên"),
                UserStatus.ACTIVE,
                new EmployeeId(1L)
        );

        nonAdminUser = new User(
                new UserId(2L),
                "pm_user",
                "hash",
                new Role(new RoleId(2L), com.hrm.employeemanagement.domain.role.RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                new EmployeeId(2L)
        );
    }

    @Test
    @DisplayName("TC-03: Người dùng không phải quản trị viên mở chức năng cấu hình -> Từ chối truy cập và ghi nhật ký")
    void tc03_nonAdminAccessDenied_logsAccessDenied() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(nonAdminUser);
        when(permissionQueryPort.hasPermission(2L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(false);

        PermissionDeniedException exception = assertThrows(
                PermissionDeniedException.class,
                () -> service.getConfig()
        );

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("NOTIFICATION_DEDUPLICATION_MANAGE"));

        // Kiểm tra log bảo mật ACCESS_DENIED_NOTIFICATION_DEDUP_CONFIG được ghi
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals(2L, savedLog.getUserId());
        assertEquals("ACCESS_DENIED_NOTIFICATION_DEDUP_CONFIG", savedLog.getAction());

        verify(configRepositoryPort, never()).loadConfig();
    }

    @Test
    @DisplayName("Quản trị viên (VT-06) truy cập thành công lấy thông tin cấu hình")
    void adminAccess_success() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);
        when(configRepositoryPort.loadConfig()).thenReturn(NotificationDedupConfig.defaultConfig());

        NotificationDedupConfigResult result = service.getConfig();

        assertNotNull(result);
        assertEquals(true, result.isEnabled());
        assertEquals(7, result.dedupWindowDays());
        assertEquals(60, result.scanIntervalMinutes());
        verify(deniedAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Quản trị viên cập nhật cấu hình -> Ghi nhận người thực hiện, nội dung và thời điểm vào lịch sử")
    void tc04_updateConfig_success_recordsHistory() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);

        NotificationDedupConfig currentConfig = NotificationDedupConfig.defaultConfig();
        when(configRepositoryPort.loadConfig()).thenReturn(currentConfig);
        when(configRepositoryPort.saveConfig(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateNotificationDedupConfigCommand command = new UpdateNotificationDedupConfigCommand(
                false,
                14,
                120
        );

        NotificationDedupConfigResult result = service.updateConfig(command, 1L);

        assertNotNull(result);
        assertEquals(false, result.isEnabled());
        assertEquals(14, result.dedupWindowDays());
        assertEquals(120, result.scanIntervalMinutes());

        // Kiểm tra lưu lịch sử thay đổi (TC-04)
        ArgumentCaptor<NotificationDedupConfigHistory> historyCaptor =
                ArgumentCaptor.forClass(NotificationDedupConfigHistory.class);
        verify(configRepositoryPort).saveHistory(historyCaptor.capture());

        NotificationDedupConfigHistory history = historyCaptor.getValue();
        assertEquals(1L, history.getActorUserId());
        assertEquals("UPDATE_DEDUP_CONFIG", history.getAction());
        assertNotNull(history.getPreviousValue());
        assertNotNull(history.getNewValue());
        assertNotNull(history.getCreatedAt());
        assertNotNull(history.getChangeSummary());
    }

    @Test
    @DisplayName("triggerManualScan: Cả year và weekNumber là null -> Quét tuần hiện tại")
    void triggerManualScan_bothNull_scansCurrentWeek() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);

        OverloadScanResult mockResult = new OverloadScanResult(10, 2, 2, 0, 0);
        when(scanOverloadAndAlertUseCase.scanCurrentWeek()).thenReturn(mockResult);

        OverloadScanResult result = service.triggerManualScan(null, null);

        assertNotNull(result);
        assertEquals(10, result.totalScanned());
        assertEquals(2, result.newlyAlertedCount());
        verify(scanOverloadAndAlertUseCase).scanCurrentWeek();
    }

    @Test
    @DisplayName("triggerManualScan: Cung cấp đầy đủ year và weekNumber hợp lệ -> Quét tuần chỉ định")
    void triggerManualScan_validYearAndWeek_scansSpecifiedWeek() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);

        OverloadScanResult mockResult = new OverloadScanResult(5, 1, 1, 0, 0);
        when(scanOverloadAndAlertUseCase.scanAndAlert(2026, 38)).thenReturn(mockResult);

        OverloadScanResult result = service.triggerManualScan(2026, 38);

        assertNotNull(result);
        assertEquals(1, result.newlyAlertedCount());
        verify(scanOverloadAndAlertUseCase).scanAndAlert(2026, 38);
    }

    @Test
    @DisplayName("triggerManualScan: Chỉ truyền một trong hai tham số -> Ném IllegalArgumentException (Bad Request)")
    void triggerManualScan_onlyOneParamProvided_throwsIllegalArgumentException() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.triggerManualScan(2026, null));
        assertThrows(IllegalArgumentException.class, () -> service.triggerManualScan(null, 38));
    }

    @Test
    @DisplayName("triggerManualScan: Tuần ISO không hợp lệ cho năm -> Ném IllegalArgumentException")
    void triggerManualScan_invalidWeekForYear_throwsIllegalArgumentException() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(adminUser);
        when(permissionQueryPort.hasPermission(1L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(true);

        // Năm 2026 chỉ có 53 tuần? Năm 2026 thực tế có 53 tuần. Năm 2025 có 52 tuần.
        // Tuần 53 của năm 2025 sẽ không hợp lệ.
        assertThrows(IllegalArgumentException.class, () -> service.triggerManualScan(2025, 53));
        assertThrows(IllegalArgumentException.class, () -> service.triggerManualScan(2026, 0));
        assertThrows(IllegalArgumentException.class, () -> service.triggerManualScan(2026, 54));
    }

    @Test
    @DisplayName("triggerManualScan: Người dùng không có quyền gọi -> Bị từ chối và ghi log bảo mật")
    void triggerManualScan_unauthorized_deniedAndLogsAudit() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(nonAdminUser);
        when(permissionQueryPort.hasPermission(2L, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE))
                .thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.triggerManualScan(null, null));
        verify(deniedAuditLogPort).save(any());
        verify(scanOverloadAndAlertUseCase, never()).scanCurrentWeek();
    }
}
