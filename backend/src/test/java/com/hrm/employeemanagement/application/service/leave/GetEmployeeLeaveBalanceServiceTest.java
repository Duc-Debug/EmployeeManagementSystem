package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NCL-05-CN-005: Application Service Test - GetEmployeeLeaveBalanceService (RBAC & Audit)")
class GetEmployeeLeaveBalanceServiceTest {

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
    @Mock
    private SaveAuditLogInNewTransactionPort auditLogRepository;

    private GetEmployeeLeaveBalanceService service;

    @BeforeEach
    void setUp() {
        service = new GetEmployeeLeaveBalanceService(
                loadEmployeePort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService,
                auditLogRepository
        );
    }

    private Employee createMockEmployee(Long empId, Long userId) {
        return new Employee(
                new EmployeeId(empId),
                new UserId(userId),
                1L,
                "EMP" + empId,
                "Employee " + empId,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("TC-03: Quản lý có quyền LEAVE_BALANCE_MANAGE xem thành công quỹ phép của cấp dưới")
    void testGetEmployeeLeaveBalance_AsManager_Success() {
        Long managerUserId = 999L;
        Long managerEmpId = 1L;
        Long targetEmpId = 20L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(managerUserId);
        when(loadEmployeePort.findByUserId(new UserId(managerUserId)))
                .thenReturn(Optional.of(createMockEmployee(managerEmpId, managerUserId)));
        when(authorizationService.hasPermission(PermissionCode.LEAVE_BALANCE_MANAGE)).thenReturn(true);
        when(loadEmployeePort.findById(new EmployeeId(targetEmpId)))
                .thenReturn(Optional.of(createMockEmployee(targetEmpId, 200L)));

        LeaveBalance balance = new LeaveBalance(1L, targetEmpId, year, new BigDecimal("12.00"), BigDecimal.ZERO);
        when(loadLeaveBalancePort.findByEmployeeIdAndYear(targetEmpId, year)).thenReturn(Optional.of(balance));
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(targetEmpId, year)).thenReturn(List.of());

        LeaveBalanceResult result = service.getEmployeeLeaveBalance(targetEmpId, year);

        assertNotNull(result);
        assertEquals(targetEmpId, result.employeeId());
        assertEquals(new BigDecimal("12.0"), result.remainingDays());
    }

    @Test
    @DisplayName("AC-03 & TC-03: Nhân viên thường không có quyền LEAVE_BALANCE_MANAGE cố xem của người khác -> Bị chặn và ghi Audit Log")
    void testGetEmployeeLeaveBalance_Unauthorized_ThrowsAndLogsAudit() {
        Long empUserId = 100L;
        Long empId = 10L;
        Long targetEmpId = 20L; // Nhân viên khác

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(empUserId);
        when(loadEmployeePort.findByUserId(new UserId(empUserId)))
                .thenReturn(Optional.of(createMockEmployee(empId, empUserId)));
        when(authorizationService.hasPermission(PermissionCode.LEAVE_BALANCE_MANAGE)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () ->
                service.getEmployeeLeaveBalance(targetEmpId, 2026));

        // Xác minh AC-03: Audit log được ghi nhận lại hành vi truy cập trái phép
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("ACCESS_DENIED_LEAVE_BALANCE", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Nhân viên tự xem số ngày phép của chính mình qua endpoint này -> Cho phép")
    void testGetEmployeeLeaveBalance_SelfAccess_Allowed() {
        Long empUserId = 100L;
        Long empId = 10L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(empUserId);
        when(loadEmployeePort.findByUserId(new UserId(empUserId)))
                .thenReturn(Optional.of(createMockEmployee(empId, empUserId)));
        // Không có LEAVE_BALANCE_MANAGE nhưng empId trùng targetEmpId
        when(loadEmployeePort.findById(new EmployeeId(empId)))
                .thenReturn(Optional.of(createMockEmployee(empId, empUserId)));
        when(loadLeaveBalancePort.findByEmployeeIdAndYear(empId, year))
                .thenReturn(Optional.of(new LeaveBalance(1L, empId, year, new BigDecimal("12.00"), BigDecimal.ZERO)));
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(empId, year)).thenReturn(List.of());

        LeaveBalanceResult result = service.getEmployeeLeaveBalance(empId, year);
        assertNotNull(result);
        assertEquals(empId, result.employeeId());
    }
}
