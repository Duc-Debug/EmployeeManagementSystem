package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CancelUnavailabilityDeclarationServiceTest {

    private LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private AuthorizationService authorizationService;
    private SaveAuditLogPort saveAuditLogPort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private SaveWeeklyProjectAllocationPort saveAllocationPort;
    private LoadHolidaysPort loadHolidaysPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    private CancelUnavailabilityDeclarationService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        saveUnavailabilityPort = mock(SaveUnavailabilityDeclarationPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadUserPort = mock(LoadUserPort.class);
        authorizationService = mock(AuthorizationService.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        saveWeeklyAvailabilityPort = mock(SaveWeeklyAvailabilityPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        saveAllocationPort = mock(SaveWeeklyProjectAllocationPort.class);
        loadHolidaysPort = mock(LoadHolidaysPort.class);
        loadApprovedLeavesPort = mock(LoadApprovedLeavesPort.class);

        service = new CancelUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveAuditLogPort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                null
        );
    }

    @Test
    @DisplayName("Hủy đơn PENDING thành công -> Đổi trạng thái sang CANCELLED và ghi Audit Log")
    void testCancelPendingDeclarationSuccess() {
        Long currentUserId = 10L;
        Long employeeId = 5L;
        Long declarationId = 100L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(currentUser.getDataScope()).thenReturn(DataScope.SELF);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId), new UserId(currentUserId), 1L, "EMP005", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        LocalDate start = LocalDate.of(2026, 9, 22);
        LocalDate end = LocalDate.of(2026, 9, 23);
        UnavailabilityDeclaration decl = new UnavailabilityDeclaration(
                declarationId, employeeId, start, end, UnavailabilityReasonType.TRAINING, "Học tập",
                BigDecimal.valueOf(16), UnavailabilityStatus.PENDING, null, null, null, null, null, 0L
        );
        when(loadUnavailabilityPort.findByIdForUpdate(declarationId)).thenReturn(Optional.of(decl));
        when(saveUnavailabilityPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UnavailabilityDeclarationResult result = service.cancel(declarationId);

        assertNotNull(result);
        assertEquals(UnavailabilityStatus.CANCELLED, result.status());

        verify(saveAuditLogPort).save(any(AuditLog.class));
        verify(saveWeeklyAvailabilityPort, never()).save(any());
    }

    @Test
    @DisplayName("Hủy đơn APPROVED tương lai -> Hoàn trả capacity tuần và xóa cờ quá tải nếu có")
    void testCancelApprovedFutureDeclarationRestoresCapacityAndClearsOverload() {
        Long currentUserId = 10L;
        Long employeeId = 5L;
        Long declarationId = 101L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(currentUser.getDataScope()).thenReturn(DataScope.SELF);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId), new UserId(currentUserId), 1L, "EMP005", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        LocalDate start = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(1);
        UnavailabilityDeclaration decl = new UnavailabilityDeclaration(
                declarationId, employeeId, start, end, UnavailabilityReasonType.TRAINING, "Học tập",
                BigDecimal.valueOf(16), UnavailabilityStatus.APPROVED, 99L, "Duyệt", LocalDateTime.now(), null, null, 0L
        );
        when(loadUnavailabilityPort.findByIdForUpdate(declarationId)).thenReturn(Optional.of(decl));
        when(saveUnavailabilityPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        YearWeek yw = YearWeek.from(start);
        WeeklyAvailability currentAvail = WeeklyAvailability.createCalculated(employeeId, yw, 40, 0, BigDecimal.ZERO);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yw)).thenReturn(Optional.of(currentAvail));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // Sau khi hủy, không còn đơn APPROVED nào trong tuần đó
        when(loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(eq(employeeId), any(), any()))
                .thenReturn(List.of());

        // Phân bổ dự án 35h đang bị quá tải (vì trước đó capacity là 24h)
        WeeklyProjectAllocation allocation = new WeeklyProjectAllocation(
                1L, employeeId, 20L, yw, BigDecimal.valueOf(35.00));
        allocation.markOverloaded("Quá tải", 99L, LocalDateTime.now());
        assertTrue(allocation.isOverloaded());
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of(allocation));

        UnavailabilityDeclarationResult result = service.cancel(declarationId);

        assertNotNull(result);
        assertEquals(UnavailabilityStatus.CANCELLED, result.status());

        // Capacity được phục hồi về 40h
        ArgumentCaptor<WeeklyAvailability> availCaptor = ArgumentCaptor.forClass(WeeklyAvailability.class);
        verify(saveWeeklyAvailabilityPort).save(availCaptor.capture());
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), availCaptor.getValue().getNetAvailableHours());

        // Cờ quá tải được clear vì 35h <= 40h capacity
        ArgumentCaptor<WeeklyProjectAllocation> allocCaptor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort).save(allocCaptor.capture());
        assertFalse(allocCaptor.getValue().isOverloaded());
    }

    @Test
    @DisplayName("Hủy đơn APPROVED trong quá khứ -> Ném ngoại lệ InvalidUnavailabilityPeriodException")
    void testCancelPastApprovedDeclarationThrowsException() {
        Long currentUserId = 10L;
        Long employeeId = 5L;
        Long declarationId = 102L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(currentUser.getDataScope()).thenReturn(DataScope.SELF);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId), new UserId(currentUserId), 1L, "EMP005", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        LocalDate pastDate = LocalDate.now().minusDays(5);
        UnavailabilityDeclaration decl = new UnavailabilityDeclaration(
                declarationId, employeeId, pastDate, pastDate, UnavailabilityReasonType.TRAINING, "Đã xong",
                BigDecimal.valueOf(8), UnavailabilityStatus.APPROVED, 99L, "Duyệt", LocalDateTime.now(), null, null, 0L
        );
        when(loadUnavailabilityPort.findByIdForUpdate(declarationId)).thenReturn(Optional.of(decl));

        assertThrows(InvalidUnavailabilityPeriodException.class, () -> service.cancel(declarationId));
    }
}