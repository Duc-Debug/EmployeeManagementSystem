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
    private com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort loadUserPort;
    @Mock
    private com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort loadOrgUnitPort;
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
    @Mock
    private com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort loadWorkingCalendarPort;
    @Mock
    private com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort loadHolidaysPort;

    private GetEmployeeLeaveBalanceService service;

    @BeforeEach
    void setUp() {
        service = new GetEmployeeLeaveBalanceService(
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService,
                auditLogRepository,
                loadWorkingCalendarPort,
                loadHolidaysPort
        );
    }

    private Employee createMockEmployee(Long empId, Long userId, Long orgUnitId) {
        return new Employee(
                new EmployeeId(empId),
                new UserId(userId),
                orgUnitId,
                "EMP" + empId,
                "Employee " + empId,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private com.hrm.employeemanagement.domain.user.User createMockUser(
            Long userId,
            com.hrm.employeemanagement.domain.role.RoleCode roleCode,
            com.hrm.employeemanagement.domain.authorization.DataScope dataScope,
            Long scopeOrgUnitId,
            Long employeeId
    ) {
        return new com.hrm.employeemanagement.domain.user.User(
                new UserId(userId),
                "user_" + userId,
                "hashed",
                new com.hrm.employeemanagement.domain.role.Role(new com.hrm.employeemanagement.domain.role.RoleId(1L), roleCode, roleCode.getName()),
                com.hrm.employeemanagement.domain.user.UserStatus.ACTIVE,
                employeeId != null ? new EmployeeId(employeeId) : null,
                dataScope,
                scopeOrgUnitId,
                1L
        );
    }

    @Test
    @DisplayName("TC-03: Quản lý nguồn lực (VT-03) có DataScope ORG_BRANCH xem thành công nhân viên trong chi nhánh")
    void testGetEmployeeLeaveBalance_AsVT03_BranchScope_Success() {
        Long vt03UserId = 300L;
        Long targetEmpId = 20L;
        Long branchOrgUnitId = 10L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(vt03UserId);
        when(loadUserPort.findById(new UserId(vt03UserId)))
                .thenReturn(Optional.of(createMockUser(vt03UserId, com.hrm.employeemanagement.domain.role.RoleCode.VT_03,
                        com.hrm.employeemanagement.domain.authorization.DataScope.ORGANIZATION_BRANCH, branchOrgUnitId, 3L)));

        Employee targetEmployee = createMockEmployee(targetEmpId, 200L, branchOrgUnitId);
        when(loadEmployeePort.findById(new EmployeeId(targetEmpId))).thenReturn(Optional.of(targetEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(branchOrgUnitId, branchOrgUnitId)).thenReturn(true);

        LeaveBalance balance = new LeaveBalance(1L, targetEmpId, year, new BigDecimal("12.00"), BigDecimal.ZERO);
        when(loadLeaveBalancePort.findOrCreateDefault(targetEmpId, year)).thenReturn(balance);
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(targetEmpId, year)).thenReturn(List.of());

        LeaveBalanceResult result = service.getEmployeeLeaveBalance(targetEmpId, year);

        assertNotNull(result);
        assertEquals(targetEmpId, result.employeeId());
        assertEquals(new BigDecimal("12.0"), result.remainingDays());
    }

    @Test
    @DisplayName("AC-03 & TC-03: Quản lý VT-03 cố xem nhân viên ngoài chi nhánh -> Bị chặn và ghi Audit Log")
    void testGetEmployeeLeaveBalance_VT03_OutsideBranch_ThrowsAndLogsAudit() {
        Long vt03UserId = 300L;
        Long targetEmpId = 20L;
        Long myBranchId = 10L;
        Long otherBranchId = 99L;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(vt03UserId);
        when(loadUserPort.findById(new UserId(vt03UserId)))
                .thenReturn(Optional.of(createMockUser(vt03UserId, com.hrm.employeemanagement.domain.role.RoleCode.VT_03,
                        com.hrm.employeemanagement.domain.authorization.DataScope.ORGANIZATION_BRANCH, myBranchId, 3L)));

        Employee targetEmployee = createMockEmployee(targetEmpId, 200L, otherBranchId);
        when(loadEmployeePort.findById(new EmployeeId(targetEmpId))).thenReturn(Optional.of(targetEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(otherBranchId, myBranchId)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () ->
                service.getEmployeeLeaveBalance(targetEmpId, 2026));

        // Xác minh AC-03: Audit log được ghi nhận lại hành vi truy cập trái phép
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("ACCESS_DENIED_LEAVE_BALANCE", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Nhân viên (VT-04, DataScope SELF) tự xem số ngày phép của chính mình -> Cho phép")
    void testGetEmployeeLeaveBalance_SelfAccess_Allowed() {
        Long empUserId = 100L;
        Long empId = 10L;
        int year = 2026;

        when(authorizationService.require(PermissionCode.LEAVE_BALANCE_READ)).thenReturn(empUserId);
        when(loadUserPort.findById(new UserId(empUserId)))
                .thenReturn(Optional.of(createMockUser(empUserId, com.hrm.employeemanagement.domain.role.RoleCode.VT_04,
                        com.hrm.employeemanagement.domain.authorization.DataScope.SELF, null, empId)));

        Employee selfEmployee = createMockEmployee(empId, empUserId, 1L);
        when(loadEmployeePort.findById(new EmployeeId(empId))).thenReturn(Optional.of(selfEmployee));
        when(loadLeaveBalancePort.findOrCreateDefault(empId, year))
                .thenReturn(new LeaveBalance(1L, empId, year, new BigDecimal("12.00"), BigDecimal.ZERO));
        when(loadLeaveRequestPort.findByEmployeeIdAndYear(empId, year)).thenReturn(List.of());

        LeaveBalanceResult result = service.getEmployeeLeaveBalance(empId, year);
        assertNotNull(result);
        assertEquals(empId, result.employeeId());
    }
}
