package com.hrm.employeemanagement.application.service.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAvailabilityService Application Tests (CN-003 & QTN-10)")
class WeeklyAvailabilityServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private AuthorizationService authorizationService;

    private WeeklyAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyAvailabilityService(
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Khai báo giờ chuẩn tuần thành công (TC-CMD-01)")
    void declareAvailability_Success() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(1L), 1L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(eq(employeeId), any(YearWeek.class)))
                .thenReturn(Optional.empty());
        when(loadHolidaysPort.getHolidayDatesBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(saveWeeklyAvailabilityPort.save(any(WeeklyAvailability.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeclareWeeklyAvailabilityCommand command = new DeclareWeeklyAvailabilityCommand(
                employeeId, 2026, 36, 40);

        WeeklyAvailabilityResult result = service.execute(command);

        assertNotNull(result);
        assertEquals(employee.getIdValue(), result.employeeId());
        assertEquals(2026, result.year());
        assertEquals(36, result.weekNumber());
        assertEquals(40, result.standardHours());
        assertEquals(new BigDecimal("40.00"), result.netAvailableHours());

        verify(authorizationService).require(PermissionCode.EMPLOYEE_UPDATE);
        verify(saveWeeklyAvailabilityPort).save(any(WeeklyAvailability.class));
    }

    @Test
    @DisplayName("Khai báo thất bại khi không tìm thấy nhân sự (TC-CMD-03)")
    void declareAvailability_EmployeeNotFound() {
        Long invalidEmployeeId = 999L;
        when(loadEmployeePort.findById(new EmployeeId(invalidEmployeeId))).thenReturn(Optional.empty());

        DeclareWeeklyAvailabilityCommand command = new DeclareWeeklyAvailabilityCommand(
                invalidEmployeeId, 2026, 36, 40);

        assertThrows(EmployeeNotFoundException.class, () -> service.execute(command));
        verify(authorizationService).require(PermissionCode.EMPLOYEE_UPDATE);
        verify(saveWeeklyAvailabilityPort, never()).save(any());
    }

    @Test
    @DisplayName("Tính toán năng lực khả dụng theo QTN-10 có ngày lễ và nghỉ phép đã duyệt")
    void calculateWeeklyCapacity_WithHolidaysAndApprovedLeaves() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(1L), 1L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(2026, 36);
        LocalDate wednesday = yearWeek.getStartDate().plusDays(2);

        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(eq(employeeId), any(YearWeek.class)))
                .thenReturn(Optional.empty());
        when(loadHolidaysPort.getHolidayDatesBetween(any(), any())).thenReturn(List.of(wednesday)); // 1 ngày lễ = 8h
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any()))
                .thenReturn(new BigDecimal("8.00")); // 1 ngày phép đã duyệt = 8h

        CalculateWeeklyCapacityQuery query = new CalculateWeeklyCapacityQuery(employeeId, 2026, 36);
        WeeklyAvailabilityResult result = service.calculate(query);

        assertNotNull(result);
        assertEquals(40, result.standardHours());
        assertEquals(8, result.holidayHours());
        assertEquals(new BigDecimal("8.00"), result.approvedLeaveHours());
        // Khả dụng = 40 - 8 (lễ) - 8 (phép duyệt) = 24h
        assertEquals(new BigDecimal("24.00"), result.netAvailableHours());

        verify(authorizationService).require(PermissionCode.EMPLOYEE_READ);
    }
}
