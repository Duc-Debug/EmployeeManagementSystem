package com.hrm.employeemanagement.application.service.notification.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.LoadWeeklyOverloadCandidatesPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupRepositoryPort;
import com.hrm.employeemanagement.domain.notification.dedup.DedupRecordStatus;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupRecord;

class OverloadAlertScanServiceTest {

    private NotificationDedupConfigRepositoryPort configRepositoryPort;
    private NotificationDedupRepositoryPort dedupRepositoryPort;
    private LoadWeeklyOverloadCandidatesPort loadCandidatesPort;
    private CreateNotificationEventUseCase createNotificationEventUseCase;
    private Clock fixedClock;

    private OverloadAlertScanService scanService;

    @BeforeEach
    void setUp() {
        configRepositoryPort = mock(NotificationDedupConfigRepositoryPort.class);
        dedupRepositoryPort = mock(NotificationDedupRepositoryPort.class);
        loadCandidatesPort = mock(LoadWeeklyOverloadCandidatesPort.class);
        createNotificationEventUseCase = mock(CreateNotificationEventUseCase.class);

        fixedClock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneId.of("UTC"));

        scanService = new OverloadAlertScanService(
                configRepositoryPort,
                dedupRepositoryPort,
                loadCandidatesPort,
                createNotificationEventUseCase,
                fixedClock
        );

        when(configRepositoryPort.loadConfig()).thenReturn(NotificationDedupConfig.defaultConfig());
    }

    @Test
    @DisplayName("TC-01: Tác vụ nền đã gửi cảnh báo -> Chạy lại sau một giờ -> Không gửi lại cho cùng tuần và cùng nhân sự")
    void tc01_subsequentScan_skipsDuplicateAlert() {
        EmployeeWeeklyOverloadCandidate candidate = new EmployeeWeeklyOverloadCandidate(
                101L,
                "NV001",
                "Nguyễn Văn A",
                2026,
                38,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(40),
                true,
                List.of(201L)
        );

        when(loadCandidatesPort.loadOverloadCandidates(2026, 38)).thenReturn(List.of(candidate));

        // Lần 1: Chưa có khóa active -> Hệ thống tạo khóa và gửi cảnh báo
        when(dedupRepositoryPort.findActiveByDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.empty());

        OverloadScanResult firstResult = scanService.scanAndAlert(2026, 38);

        assertEquals(1, firstResult.totalScanned());
        assertEquals(1, firstResult.overloadedCount());
        assertEquals(1, firstResult.newlyAlertedCount());
        assertEquals(0, firstResult.skippedDedupCount());
        verify(dedupRepositoryPort, times(1)).save(any(NotificationDedupRecord.class));
        verify(createNotificationEventUseCase, times(1)).execute(any(CreateNotificationEventCommand.class));

        // Lần 2 (chạy lại sau 1 giờ theo TC-01): Khóa active đã tồn tại
        NotificationDedupRecord existingActiveRecord = NotificationDedupRecord.createActive(
                "OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201",
                "OVERLOAD_WARNING",
                "EMPLOYEE",
                "101",
                "2026-W38",
                201L,
                LocalDateTime.now(fixedClock),
                LocalDateTime.now(fixedClock).plusDays(7)
        );

        when(dedupRepositoryPort.findActiveByDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.of(existingActiveRecord));

        OverloadScanResult secondResult = scanService.scanAndAlert(2026, 38);

        assertEquals(1, secondResult.totalScanned());
        assertEquals(1, secondResult.overloadedCount());
        assertEquals(0, secondResult.newlyAlertedCount());
        assertEquals(1, secondResult.skippedDedupCount()); // Bỏ qua trùng lặp thành công!

        // createNotificationEventUseCase KHÔNG được gọi thêm lần nào nữa (vẫn là 1 lần từ trước)
        verify(createNotificationEventUseCase, times(1)).execute(any(CreateNotificationEventCommand.class));
    }

    @Test
    @DisplayName("TC-02: Tuần đó thoát quá tải rồi lại quá tải lần nữa -> Coi là sự kiện mới và gửi cảnh báo lần nữa")
    void tc02_exitOverloadThenOverloadAgain_treatedAsNewEventAndResent() {
        // Giai đoạn 1: Nhân sự thoát quá tải (allocated 35h <= available 40h)
        EmployeeWeeklyOverloadCandidate resolvedCandidate = new EmployeeWeeklyOverloadCandidate(
                101L,
                "NV001",
                "Nguyễn Văn A",
                2026,
                38,
                BigDecimal.valueOf(35),
                BigDecimal.valueOf(40),
                false,
                List.of(201L)
        );

        NotificationDedupRecord oldActiveRecord = NotificationDedupRecord.createActive(
                "OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201",
                "OVERLOAD_WARNING",
                "EMPLOYEE",
                "101",
                "2026-W38",
                201L,
                LocalDateTime.now(fixedClock),
                LocalDateTime.now(fixedClock).plusDays(7)
        );

        when(loadCandidatesPort.loadOverloadCandidates(2026, 38)).thenReturn(List.of(resolvedCandidate));
        when(dedupRepositoryPort.findActiveByTargetAndWeek("EMPLOYEE", "101", "2026-W38"))
                .thenReturn(List.of(oldActiveRecord));

        OverloadScanResult resolveResult = scanService.scanAndAlert(2026, 38);

        assertEquals(1, resolveResult.resolvedCount());
        verify(dedupRepositoryPort).resolveActiveRecords(eq("EMPLOYEE"), eq("101"), eq("2026-W38"), any(LocalDateTime.class));

        // Giai đoạn 2: Sau khi đã thoát tải, nhân sự bị gán thêm việc và quá tải trở lại trong cùng tuần
        EmployeeWeeklyOverloadCandidate reOverloadedCandidate = new EmployeeWeeklyOverloadCandidate(
                101L,
                "NV001",
                "Nguyễn Văn A",
                2026,
                38,
                BigDecimal.valueOf(48),
                BigDecimal.valueOf(40),
                true,
                List.of(201L)
        );

        when(loadCandidatesPort.loadOverloadCandidates(2026, 38)).thenReturn(List.of(reOverloadedCandidate));
        // Vì bản ghi cũ đã được resolve (status = INACTIVE, active_dedup_key = null), findActiveByDedupKey trả về Empty
        when(dedupRepositoryPort.findActiveByDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.empty());

        OverloadScanResult reAlertResult = scanService.scanAndAlert(2026, 38);

        assertEquals(1, reAlertResult.newlyAlertedCount());
        assertEquals(0, reAlertResult.skippedDedupCount());

        // Kiểm tra hệ thống tạo bản ghi mới và gửi cảnh báo lại (TC-02)
        verify(dedupRepositoryPort, times(1)).save(any(NotificationDedupRecord.class));
        verify(createNotificationEventUseCase, times(1)).execute(any(CreateNotificationEventCommand.class));
    }

    @Test
    @DisplayName("Khi cơ chế chống gửi trùng bị tắt trong cấu hình -> Bỏ qua toàn bộ tác vụ rà soát")
    void testScan_whenConfigDisabled_skipsExecution() {
        NotificationDedupConfig disabledConfig = new NotificationDedupConfig(
                1L,
                false,
                7,
                60,
                1L,
                LocalDateTime.now(fixedClock),
                1L
        );
        when(configRepositoryPort.loadConfig()).thenReturn(disabledConfig);

        OverloadScanResult result = scanService.scanAndAlert(2026, 38);

        assertEquals(0, result.totalScanned());
        verify(loadCandidatesPort, never()).loadOverloadCandidates(any(int.class), any(int.class));
        verify(createNotificationEventUseCase, never()).execute(any());
    }
}
