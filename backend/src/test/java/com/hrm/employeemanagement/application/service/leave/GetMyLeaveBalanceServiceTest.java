package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NCL-05-CN-005: Application Service Test - GetMyLeaveBalanceService")
class GetMyLeaveBalanceServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadLeaveBalancePort loadLeaveBalancePort;
    @Mock
    private SaveLeaveBalancePort saveLeaveBalancePort;
    @Mock
    private LoadLeaveRequestPort loadLeaveRequestPort;
    @Mock
    private AuthorizationService authorizationService;

    private GetMyLeaveBalanceService service;

    @BeforeEach
    void setUp() {
        service = new GetMyLeaveBalanceService(
                loadEmployeePort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService
        );
    }

    private Employee createMockEmployee(Long empId, Long userId) {
        return new Employee(
                new EmployeeId(empId),
                new UserId(userId),
                1L,
                "EMP001",
                "Nguyen Van A",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private LeaveRequest createLeaveRequest(Long id, Long empId, LeaveStatus status, int days) {
        return new LeaveRequest(
                id, empId, LeaveType.ANNUAL,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, days),
                days, new BigDecimal(days * 8), "Lý do nghỉ", status,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("TC-01: Lấy thành công số ngày phép cá nhân khi đã có bản ghi LeaveBalance trong DB")
    void testGetMyLeaveBalance_WithExistingBalance() {
        Long userId = 100L;
        Long empId = 10L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));

        LeaveBalance balance = new LeaveBalance(1L, empId, year, new BigDecimal("12.00"), new BigDecimal("2.00"));
        when(loadLeaveBalancePort.findByEmployeeIdAndYear(empId, year)).thenReturn(Optional.of(balance));

        // Đã nghỉ 3 ngày APPROVED và đang chờ 2 ngày PENDING
        List<LeaveRequest> requests = List.of(
                createLeaveRequest(1L, empId, LeaveStatus.APPROVED, 3),
                createLeaveRequest(2L, empId, LeaveStatus.PENDING, 2)
        );
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(empId, year)).thenReturn(requests);

        LeaveBalanceResult result = service.getMyLeaveBalance(year);

        assertNotNull(result);
        assertEquals(empId, result.employeeId());
        assertEquals(year, result.year());
        assertEquals(0, new BigDecimal("12.0").compareTo(result.entitledDays()));
        assertEquals(0, new BigDecimal("2.0").compareTo(result.carriedOverDays()));
        assertEquals(0, new BigDecimal("14.0").compareTo(result.totalAllocatedDays()));
        assertEquals(0, new BigDecimal("3.0").compareTo(result.usedDays()));
        assertEquals(0, new BigDecimal("2.0").compareTo(result.pendingDays()));
        assertEquals(0, new BigDecimal("9.0").compareTo(result.remainingDays()));
    }

    @Test
    @DisplayName("TC-01: Tự động gán 12 ngày mặc định khi nhân viên chưa có bản ghi trong bảng employee_leave_balances")
    void testGetMyLeaveBalance_DefaultsTo12Days() {
        Long userId = 100L;
        Long empId = 10L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));
        when(loadLeaveBalancePort.findByEmployeeIdAndYear(empId, year)).thenReturn(Optional.empty());
        when(saveLeaveBalancePort.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(empId, year)).thenReturn(List.of());

        LeaveBalanceResult result = service.getMyLeaveBalance(year);

        assertEquals(0, new BigDecimal("12.0").compareTo(result.entitledDays()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.carriedOverDays()));
        assertEquals(0, new BigDecimal("12.0").compareTo(result.totalAllocatedDays()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.usedDays()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.pendingDays()));
        assertEquals(0, new BigDecimal("12.0").compareTo(result.remainingDays()));
    }

    @Test
    @DisplayName("Ném EmployeeNotFoundException khi user không có hồ sơ nhân viên")
    void testGetMyLeaveBalance_EmployeeNotFound() {
        Long userId = 100L;
        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.getMyLeaveBalance(null));
    }
}
