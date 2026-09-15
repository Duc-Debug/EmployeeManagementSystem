package com.hrm.employeemanagement.application.service.report.capacityforecast;

import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastQuery;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult.ForecastStatus;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
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
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("GetCapacityForecastService Unit Tests (NCL-10-CN-004)")
class GetCapacityForecastServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadHolidaysPort loadHolidaysPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private LoadWorkingCalendarPort loadWorkingCalendarPort;
    private LoadResourceReservationPort loadReservationPort;
    private SaveAuditLogPort saveAuditLogPort;

    private GetCapacityForecastService service;

    private final Long USER_VT01_ID = 100L;
    private final Long USER_VT03_ID = 200L;

    private final Long ORG_UNIT_1_ID = 10L;
    private final Long ORG_UNIT_2_ID = 20L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        loadHolidaysPort = mock(LoadHolidaysPort.class);
        loadApprovedLeavesPort = mock(LoadApprovedLeavesPort.class);
        loadWorkingCalendarPort = mock(LoadWorkingCalendarPort.class);
        loadReservationPort = mock(LoadResourceReservationPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new GetCapacityForecastService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                loadReservationPort,
                saveAuditLogPort
        );

        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        OrgUnit unit1 = mock(OrgUnit.class);
        when(unit1.getUnitName()).thenReturn("Trung tâm Software 1");
        when(unit1.getTreePath()).thenReturn("/10/");
        when(loadOrgUnitPort.findById(eq(new OrgUnitId(ORG_UNIT_1_ID)))).thenReturn(Optional.of(unit1));
    }

    @Test
    @DisplayName("TC-01: Báo cáo 12 tuần trả đúng giờ khả dụng, cam kết và còn trống từng tuần")
    void execute_ShouldReturn12WeeksDataAccurately() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT01_ID);

        User vt01User = mock(User.class);
        when(vt01User.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(vt01User));

        Employee emp1 = mock(Employee.class);
        when(emp1.getIdValue()).thenReturn(1L);
        when(emp1.getStandardHoursPerWeek()).thenReturn(40);

        Employee emp2 = mock(Employee.class);
        when(emp2.getIdValue()).thenReturn(2L);
        when(emp2.getStandardHoursPerWeek()).thenReturn(40);

        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp1, emp2));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(
                101L, 1L, 500L, YearWeek.of(2026, 38), BigDecimal.valueOf(30)
        );
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(
                102L, 2L, 500L, YearWeek.of(2026, 38), BigDecimal.valueOf(20)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(alloc1, alloc2));
        when(loadReservationPort.findActiveByEmployeeIdsAndYearWeeks(any(), any())).thenReturn(List.of());

        CapacityForecastQuery query = new CapacityForecastQuery(null, 2026, 38, 12);

        // Act
        CapacityForecastResult result = service.execute(query);

        // Assert
        assertNotNull(result);
        assertEquals(12, result.weeks().size());
        assertEquals(2026, result.fromYear());
        assertEquals(38, result.fromWeek());

        CapacityForecastResult.WeeklyForecastItem week38 = result.weeks().get(0);
        assertEquals(2026, week38.year());
        assertEquals(38, week38.weekNumber());
        assertEquals(new BigDecimal("80.0"), week38.availableHours());
        assertEquals(new BigDecimal("50.0"), week38.committedHours());
        assertEquals(new BigDecimal("0.0"), week38.reservedHours());
        assertEquals(new BigDecimal("30.0"), week38.committedRemainingHours());
        assertEquals(new BigDecimal("30.0"), week38.projectedRemainingHours());
        assertEquals(new BigDecimal("62.5"), week38.committedUtilization());
        assertEquals(new BigDecimal("62.5"), week38.projectedUtilization());
        assertEquals(ForecastStatus.AVAILABLE, week38.status());

        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Giữ chỗ được trả riêng và ảnh hưởng đúng tới giờ còn trống dự kiến")
    void execute_ShouldSeparateReservedHoursFromCommittedHours() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT01_ID);

        User vt01User = mock(User.class);
        when(vt01User.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(vt01User));

        Employee emp1 = mock(Employee.class);
        when(emp1.getIdValue()).thenReturn(1L);
        when(emp1.getStandardHoursPerWeek()).thenReturn(40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp1));

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                101L, 1L, 500L, YearWeek.of(2026, 38), BigDecimal.valueOf(25)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(alloc));

        ResourceReservation reservation = new ResourceReservation(
                201L, 1000L, 1L, YearWeek.of(2026, 38), BigDecimal.valueOf(10),
                ReservationStatus.ACTIVE, null, null, "Dự án dự kiến", 100L, LocalDateTime.now(), null, null, 0L
        );
        when(loadReservationPort.findActiveByEmployeeIdsAndYearWeeks(any(), any())).thenReturn(List.of(reservation));

        CapacityForecastQuery query = new CapacityForecastQuery(null, 2026, 38, 4);

        // Act
        CapacityForecastResult result = service.execute(query);

        // Assert
        assertEquals(4, result.weeks().size());
        CapacityForecastResult.WeeklyForecastItem week1 = result.weeks().get(0);
        assertEquals(new BigDecimal("40.0"), week1.availableHours());
        assertEquals(new BigDecimal("25.0"), week1.committedHours());
        assertEquals(new BigDecimal("10.0"), week1.reservedHours());
        assertEquals(new BigDecimal("15.0"), week1.committedRemainingHours());
        assertEquals(new BigDecimal("5.0"), week1.projectedRemainingHours());
        assertEquals(new BigDecimal("62.5"), week1.committedUtilization());
        assertEquals(new BigDecimal("87.5"), week1.projectedUtilization());
        assertEquals(ForecastStatus.NEAR_FULL, week1.status());
    }

    @Test
    @DisplayName("TC-03: VT-01 & VT-03 được phép truy cập; Vai trò khác nhận PermissionDeniedException và có audit log")
    void execute_ShouldDenyUnauthorizedUserWithAuditLog() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.CAPACITY_FORECAST_REPORT_READ));

        CapacityForecastQuery query = new CapacityForecastQuery(null, 2026, 38, 12);

        // Act & Assert
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("VT-03 không xem được dữ liệu đơn vị ngoài Data Scope")
    void execute_VT03_ShouldDenyOrgUnitOutsideBranchScope() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT03_ID);

        User vt03User = mock(User.class);
        when(vt03User.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(vt03User.getScopeOrgUnitId()).thenReturn(ORG_UNIT_1_ID);
        when(loadUserPort.findById(new UserId(USER_VT03_ID))).thenReturn(Optional.of(vt03User));

        when(loadOrgUnitPort.existsInOrgUnitBranch(ORG_UNIT_2_ID, ORG_UNIT_1_ID)).thenReturn(false);

        CapacityForecastQuery query = new CapacityForecastQuery(ORG_UNIT_2_ID, 2026, 38, 12);

        // Act & Assert
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Tham số durationWeeks không hợp lệ (< 4 hoặc > 16) sẽ bị ném IllegalArgumentException")
    void execute_ShouldThrowExceptionForInvalidDuration() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT01_ID);
        User vt01User = mock(User.class);
        when(vt01User.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(vt01User));

        // Act & Assert
        CapacityForecastQuery invalidQueryLow = new CapacityForecastQuery(null, 2026, 38, 2);
        assertThrows(IllegalArgumentException.class, () -> service.execute(invalidQueryLow));

        CapacityForecastQuery invalidQueryHigh = new CapacityForecastQuery(null, 2026, 38, 20);
        assertThrows(IllegalArgumentException.class, () -> service.execute(invalidQueryHigh));
    }

    @Test
    @DisplayName("Regression: Ưu tiên dùng netAvailableHours từ WeeklyAvailability đã lưu khi có sẵn record")
    void execute_ShouldPreferSavedWeeklyAvailabilityNetAvailableHoursWhenPresent() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT01_ID);
        User vt01User = mock(User.class);
        when(vt01User.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(vt01User));

        Employee emp1 = mock(Employee.class);
        when(emp1.getIdValue()).thenReturn(1L);
        when(emp1.getStandardHoursPerWeek()).thenReturn(40);
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp1));

        // WeeklyAvailability đã lưu có standard 40h nhưng netAvailableHours = 24h
        WeeklyAvailability savedAvail = new WeeklyAvailability(
                50L, 1L, YearWeek.of(2026, 38), 40, 16, BigDecimal.ZERO, BigDecimal.valueOf(24)
        );
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of(savedAvail));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadReservationPort.findActiveByEmployeeIdsAndYearWeeks(any(), any())).thenReturn(List.of());

        CapacityForecastQuery query = new CapacityForecastQuery(null, 2026, 38, 4);

        // Act
        CapacityForecastResult result = service.execute(query);

        // Assert
        assertEquals(new BigDecimal("24.0"), result.weeks().get(0).availableHours());
    }

    @Test
    @DisplayName("Validation: Ném IllegalArgumentException khi chỉ truyền 1 trong 2 tham số fromYear hoặc fromWeek")
    void execute_ShouldThrowExceptionWhenOnlyOneOfFromYearOrFromWeekIsProvided() {
        // Arrange
        when(authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ)).thenReturn(USER_VT01_ID);
        User vt01User = mock(User.class);
        when(vt01User.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(USER_VT01_ID))).thenReturn(Optional.of(vt01User));

        // Only fromYear provided
        CapacityForecastQuery onlyYearQuery = new CapacityForecastQuery(null, 2027, null, 12);
        assertThrows(IllegalArgumentException.class, () -> service.execute(onlyYearQuery));

        // Only fromWeek provided
        CapacityForecastQuery onlyWeekQuery = new CapacityForecastQuery(null, null, 20, 12);
        assertThrows(IllegalArgumentException.class, () -> service.execute(onlyWeekQuery));
    }
}
