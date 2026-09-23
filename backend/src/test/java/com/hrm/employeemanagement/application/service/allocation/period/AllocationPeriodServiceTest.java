package com.hrm.employeemanagement.application.service.allocation.period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.CreatePeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.LockPeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;
import com.hrm.employeemanagement.application.dto.allocation.period.UnlockPeriodCommand;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationsForPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllocationPeriodService Application Unit Tests (NCL-06-CN-009)")
class AllocationPeriodServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SaveAllocationPlanningPeriodPort savePeriodPort;

    @Mock
    private LoadAllocationPlanningPeriodPort loadPeriodPort;

    @Mock
    private SaveAllocationPlanSnapshotPort saveSnapshotPort;

    @Mock
    private LoadAllocationPlanSnapshotPort loadSnapshotPort;

    @Mock
    private LoadAllocationsForPeriodPort loadAllocationsPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    private AllocationPeriodService service;

    private final Long RM_USER_ID = 3L;

    @BeforeEach
    void setUp() {
        service = new AllocationPeriodService(
                authorizationService,
                savePeriodPort,
                loadPeriodPort,
                saveSnapshotPort,
                loadSnapshotPort,
                loadAllocationsPort,
                saveAuditLogPort
        );
    }

    @Test
    @DisplayName("NCL-06-CN-009-TC-01 (Luồng thành công): Khóa kỳ kế hoạch quý đã được rà soát và lưu bản chụp")
    void tc01_shouldLockPeriodAndCreateSnapshotSuccessfully() {
        // Given
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK)).thenReturn(RM_USER_ID);

        AllocationPlanningPeriod period = new AllocationPlanningPeriod(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.OPEN, null, null, null, null, null, 1L, null, null, 0L
        );
        when(loadPeriodPort.findById(1L)).thenReturn(Optional.of(period));
        when(savePeriodPort.save(any(AllocationPlanningPeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        when(loadSnapshotPort.countSnapshotsByPeriodId(1L)).thenReturn(0);

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(
                101L, 10L, 20L, YearWeek.of(2026, 5), BigDecimal.valueOf(40.0), BigDecimal.valueOf(100.0), 0L
        );
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(
                102L, 11L, 20L, YearWeek.of(2026, 5), BigDecimal.valueOf(20.0), BigDecimal.valueOf(50.0), 0L
        );
        when(loadAllocationsPort.loadAllocationsInWeekRange(2026, 1, 13)).thenReturn(List.of(alloc1, alloc2));

        when(saveSnapshotPort.save(any(AllocationPlanSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AllocationPeriodResult result = service.lockPeriod(new LockPeriodCommand(1L));

        // Then
        assertNotNull(result);
        assertEquals(AllocationPeriodStatus.LOCKED, result.status());
        assertEquals(RM_USER_ID, result.lockedBy());
        assertNotNull(result.lockedAt());

        assertNotNull(result.latestSnapshot());
        assertEquals(1, result.latestSnapshot().snapshotVersion());
        assertEquals(2, result.latestSnapshot().totalAllocations());
        assertEquals(BigDecimal.valueOf(60.0), result.latestSnapshot().totalAllocatedHours());
        assertEquals(2, result.latestSnapshot().items().size());

        // [TC-04]: Ghi nhật ký kiểm toán lưu vết người thực hiện, nội dung và thời điểm
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(RM_USER_ID)
                        && "PLAN_PERIOD_LOCKED".equals(log.getAction())
                        && "allocation_planning_periods".equals(log.getTableName())
                        && log.getRecordId().equals(1L)
                        && log.getNewValue().contains("status=LOCKED")
                        && log.getNewValue().contains("snapshotVersion=1")
        ));
    }

    @Test
    @DisplayName("NCL-06-CN-009-TC-03 (Không có quyền): Người dùng không phải Quản lý nguồn lực bị từ chối và ghi nhật ký lần từ chối")
    void tc03_shouldDenyAccessAndLogRefusalWhenUserNotResourceManager() {
        // Given
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_LOCK));

        // When & Then
        assertThrows(PermissionDeniedException.class, () ->
                service.lockPeriod(new LockPeriodCommand(1L))
        );

        // Verify: Nhật ký kiểm toán lần từ chối được ghi lại
        verify(saveAuditLogPort).save(argThat(log ->
                "ACCESS_DENIED_PLAN_PERIOD_LOCK".equals(log.getAction())
                        && "allocation_planning_periods".equals(log.getTableName())
                        && log.getRecordId().equals(1L)
                        && log.getNewValue().contains("RESOURCE_ALLOCATION_LOCK")
        ));

        verify(savePeriodPort, never()).save(any());
        verify(saveSnapshotPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-06-CN-009-TC-04 (Lưu lịch sử): Mở lại kỳ kế hoạch thành công và ghi nhận nhật ký kiểm toán")
    void tc04_shouldUnlockPeriodAndLogAuditSuccessfully() {
        // Given
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK)).thenReturn(RM_USER_ID);

        AllocationPlanningPeriod period = new AllocationPlanningPeriod(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.LOCKED, RM_USER_ID, java.time.LocalDateTime.now(), null, null, null, 1L, null, null, 0L
        );
        when(loadPeriodPort.findById(1L)).thenReturn(Optional.of(period));
        when(savePeriodPort.save(any(AllocationPlanningPeriod.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadSnapshotPort.findLatestByPeriodId(1L)).thenReturn(Optional.empty());

        // When
        AllocationPeriodResult result = service.unlockPeriod(new UnlockPeriodCommand(1L, "Yêu cầu bổ sung phân bổ cho dự án khẩn cấp"));

        // Then
        assertNotNull(result);
        assertEquals(AllocationPeriodStatus.OPEN, result.status());
        assertEquals(RM_USER_ID, result.unlockedBy());
        assertEquals("Yêu cầu bổ sung phân bổ cho dự án khẩn cấp", result.unlockReason());
        assertNotNull(result.unlockedAt());

        // Verify: Audit log ghi nhận mở lại kỳ
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(RM_USER_ID)
                        && "PLAN_PERIOD_UNLOCKED".equals(log.getAction())
                        && "allocation_planning_periods".equals(log.getTableName())
                        && log.getRecordId().equals(1L)
                        && log.getNewValue().contains("status=OPEN")
                        && log.getNewValue().contains("Yêu cầu bổ sung phân bổ cho dự án khẩn cấp")
        ));
    }

    @Test
    @DisplayName("TC-03: Mở lại kỳ bị từ chối khi không có quyền RESOURCE_ALLOCATION_LOCK")
    void tc03_shouldDenyUnlockAndLogRefusalWhenUserNotResourceManager() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_LOCK));

        assertThrows(PermissionDeniedException.class, () ->
                service.unlockPeriod(new UnlockPeriodCommand(1L, "Lý do"))
        );

        verify(saveAuditLogPort).save(argThat(log ->
                "ACCESS_DENIED_PLAN_PERIOD_UNLOCK".equals(log.getAction())
                        && "allocation_planning_periods".equals(log.getTableName())
                        && log.getRecordId().equals(1L)
        ));
    }

    @Test
    @DisplayName("Ném AllocationPeriodNotFoundException khi khóa kỳ không tồn tại")
    void shouldThrowNotFoundWhenPeriodDoesNotExist() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK)).thenReturn(RM_USER_ID);
        when(loadPeriodPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AllocationPeriodNotFoundException.class, () ->
                service.lockPeriod(new LockPeriodCommand(999L))
        );
    }

    @Test
    @DisplayName("Tạo mới kỳ kế hoạch phân bổ thành công")
    void shouldCreatePeriodSuccessfully() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(RM_USER_ID);
        when(loadPeriodPort.existsOverlapping(2026, 1, 13, null)).thenReturn(false);
        when(savePeriodPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePeriodCommand command = new CreatePeriodCommand(
                "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13
        );

        AllocationPeriodResult result = service.createPeriod(command);

        assertNotNull(result);
        assertEquals("Kế hoạch Quý 1/2026", result.name());
        assertEquals(AllocationPeriodStatus.OPEN, result.status());
    }

    @Test
    @DisplayName("Ném InvalidAllocationPeriodException khi tạo kỳ bị trùng dải tuần")
    void shouldThrowWhenCreatingOverlappingPeriod() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(RM_USER_ID);
        when(loadPeriodPort.existsOverlapping(2026, 1, 13, null)).thenReturn(true);

        CreatePeriodCommand command = new CreatePeriodCommand(
                "Kế hoạch Quý 1/2026 trùng", AllocationPeriodType.QUARTER, 2026, 1, 13
        );

        assertThrows(InvalidAllocationPeriodException.class, () -> service.createPeriod(command));
    }

    @Test
    @DisplayName("Kiểm tra tuần bị khóa chính xác")
    void shouldCheckWeekLockCorrectly() {
        AllocationPlanningPeriod lockedPeriod = new AllocationPlanningPeriod(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.LOCKED, RM_USER_ID, java.time.LocalDateTime.now(), null, null, null, 1L, null, null, 0L
        );
        when(loadPeriodPort.findLockedPeriodsCoveringWeek(2026, 5)).thenReturn(List.of(lockedPeriod));

        PeriodLockCheckResult result = service.checkWeekLock(2026, 5);
        assertTrue(result.isLocked());
        assertEquals(1L, result.periodId());
        assertEquals("Kế hoạch Quý 1/2026", result.periodName());

        when(loadPeriodPort.findLockedPeriodsCoveringWeek(2026, 20)).thenReturn(List.of());
        PeriodLockCheckResult unlockedResult = service.checkWeekLock(2026, 20);
        assertFalse(unlockedResult.isLocked());
    }
}
