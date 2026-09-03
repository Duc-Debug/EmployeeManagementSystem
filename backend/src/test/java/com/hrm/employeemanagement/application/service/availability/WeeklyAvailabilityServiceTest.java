package com.hrm.employeemanagement.application.service.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private AuthorizationService authorizationService;

    private WeeklyAvailabilityService service;
    private User companyAdminUser;

    @BeforeEach
    void setUp() {
        service = new WeeklyAvailabilityService(
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                saveAuditLogPort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService
        );

        Role hrRole = new Role(new RoleId(1L), RoleCode.VT_05, "Nhân sự");
        companyAdminUser = new User(new UserId(1L), "hr_user", "hash", hrRole, UserStatus.ACTIVE, new EmployeeId(1L), DataScope.COMPANY, null, 0L);
    }

    @Test
    @DisplayName("Khai báo giờ chuẩn tuần thành công và ghi Audit Log (TC-01, TC-04)")
    void declareAvailability_Success_And_AuditLog_Saved() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(1L), 1L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyAdminUser));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(eq(employeeId), any(YearWeek.class)))
                .thenReturn(Optional.empty());
        when(loadHolidaysPort.getHolidaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(saveWeeklyAvailabilityPort.save(any(WeeklyAvailability.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeclareWeeklyAvailabilityCommand command = new DeclareWeeklyAvailabilityCommand(employeeId, 2026, 36, 40);
        WeeklyAvailabilityResult result = service.execute(command);

        assertNotNull(result);
        assertEquals(40, result.standardHours());
        assertEquals(new BigDecimal("40.00"), result.netAvailableHours());

        verify(authorizationService).require(PermissionCode.EMPLOYEE_UPDATE);
        verify(saveWeeklyAvailabilityPort).save(any(WeeklyAvailability.class));
        // Xác nhận đã lưu Audit Log theo TC-04
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Bán thời gian: Tính năng lực tuần dùng 20 giờ thay vì mặc định 40 giờ (TC-02)")
    void calculate_PartTime_Uses20Hours() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(1L), 1L, "EMP001", "Nguyễn Văn Bán Thời Gian", null, null, null, false, 20, EmployeeStatus.ACTIVE);

        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyAdminUser));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(eq(employeeId), any(YearWeek.class)))
                .thenReturn(Optional.empty());
        when(loadHolidaysPort.getHolidaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        CalculateWeeklyCapacityQuery query = new CalculateWeeklyCapacityQuery(employeeId, 2026, 36);
        WeeklyAvailabilityResult result = service.calculate(query);

        assertNotNull(result);
        assertEquals(20, result.standardHours());
        assertEquals(new BigDecimal("20.00"), result.netAvailableHours());
    }

    @Test
    @DisplayName("Data Scope: Người dùng nhánh khác cố truy cập nhân viên ngoài phạm vi -> Ném PermissionDeniedException (TC-03 & Security)")
    void dataScope_OrganizationBranch_DeniedWhenOutOfScope() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(2L), 10L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        Role rmRole = new Role(new RoleId(2L), RoleCode.VT_03, "Quản lý nguồn lực");
        User rmUserBranch = new User(new UserId(2L), "rm_user", "hash", rmRole, UserStatus.ACTIVE, new EmployeeId(2L), DataScope.ORGANIZATION_BRANCH, 5L, 0L);

        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUserBranch));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        // Đơn vị 10 không thuộc nhánh quản lý 5
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 5L)).thenReturn(false);

        CalculateWeeklyCapacityQuery query = new CalculateWeeklyCapacityQuery(employeeId, 2026, 36);
        assertThrows(PermissionDeniedException.class, () -> service.calculate(query));
    }

    @Test
    @DisplayName("Tính năng lực tuần trừ đúng ngày lễ linh hoạt và nghỉ phép đã duyệt theo QTN-10")
    void calculate_DeductsDynamicHolidaysAndLeaves() {
        Long employeeId = 100L;
        Employee employee = new Employee(new EmployeeId(employeeId), new UserId(1L), 1L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(2026, 36);
        LocalDate wednesday = yearWeek.getStartDate().plusDays(2);
        Holiday holiday = new Holiday(wednesday, "Lễ Quốc Khánh", 8);

        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyAdminUser));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(eq(employeeId), any(YearWeek.class)))
                .thenReturn(Optional.empty());
        when(loadHolidaysPort.getHolidaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(holiday));
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("16.00")); // 2 ngày phép

        CalculateWeeklyCapacityQuery query = new CalculateWeeklyCapacityQuery(employeeId, 2026, 36);
        WeeklyAvailabilityResult result = service.calculate(query);

        assertNotNull(result);
        assertEquals(40, result.standardHours());
        assertEquals(8, result.holidayHours());
        assertEquals(new BigDecimal("16.00"), result.approvedLeaveHours());
        // 40 - 8 - 16 = 16.00
        assertEquals(new BigDecimal("16.00"), result.netAvailableHours());
    }

    @Test
    @DisplayName("Khai báo giờ tuần thất bại khi không tìm thấy nhân viên -> Ném EmployeeNotFoundException")
    void declareAvailability_EmployeeNotFound() {
        Long employeeId = 9999L;
        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyAdminUser));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.empty());

        DeclareWeeklyAvailabilityCommand command = new DeclareWeeklyAvailabilityCommand(employeeId, 2026, 36, 40);
        assertThrows(EmployeeNotFoundException.class, () -> service.execute(command));
    }
}
