package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadDepartmentMonthlyLeavePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveCalendarItem;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentMonthlyLeaveCalendarService Application Tests (NCL-05-CN-006)")
class DepartmentMonthlyLeaveCalendarServiceTest {

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadDepartmentMonthlyLeavePort loadDepartmentMonthlyLeavePort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private DepartmentMonthlyLeaveCalendarService service;

    private User rmUser;
    private OrgUnit department;

    @BeforeEach
    void setUp() {
        service = new DepartmentMonthlyLeaveCalendarService(
                loadOrgUnitPort,
                loadEmployeePort,
                loadUserPort,
                loadDepartmentMonthlyLeavePort,
                authorizationService,
                saveAuditLogPort
        );

        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        rmUser = new User(
                new UserId(2L),
                "rm_user",
                "hash",
                rmRole,
                UserStatus.ACTIVE,
                new EmployeeId(2L),
                DataScope.ORGANIZATION_BRANCH,
                10L, // scopeOrgUnitId
                0L
        );

        department = new OrgUnit(
                new OrgUnitId(10L),
                "DEV-DEP",
                "Phòng Phát triển Phần mềm",
                OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L),
                "/1/10/",
                2,
                OrgUnitStatus.ACTIVE,
                "Bộ phận kỹ thuật",
                2L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Bộ phận có sáu đơn nghỉ trong tháng -> Hệ thống hiện đủ sáu khoảng nghỉ trên lịch tháng")
    void tc01_successFlow_sixLeaveRequestsDisplayed() {
        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(10L, 2026, 9, 0.50);

        when(authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(department));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        // 6 nhân viên trong bộ phận
        List<Employee> employees = List.of(
                createEmployee(1L, "EMP-01", "Nguyễn Văn A"),
                createEmployee(2L, "EMP-02", "Trần Thị B"),
                createEmployee(3L, "EMP-03", "Lê Văn C"),
                createEmployee(4L, "EMP-04", "Phạm Thị D"),
                createEmployee(5L, "EMP-05", "Hoàng Văn E"),
                createEmployee(6L, "EMP-06", "Đỗ Thị F")
        );
        when(loadEmployeePort.findActiveByOrgUnitId(10L)).thenReturn(employees);

        // Sáu đơn nghỉ mô phỏng
        List<LeaveCalendarItem> sixLeaves = List.of(
                new LeaveCalendarItem(101L, 1L, "EMP-01", "Nguyễn Văn A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm"),
                new LeaveCalendarItem(102L, 2L, "EMP-02", "Trần Thị B", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8), LeaveStatus.PENDING, new BigDecimal("16.00"), "ANNUAL", "Việc gia đình"),
                new LeaveCalendarItem(103L, 3L, "EMP-03", "Lê Văn C", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ mát"),
                new LeaveCalendarItem(104L, 4L, "EMP-04", "Phạm Thị D", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15), LeaveStatus.APPROVED, new BigDecimal("8.00"), "SICK", "Khám sức khỏe"),
                new LeaveCalendarItem(105L, 5L, "EMP-05", "Hoàng Văn E", LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22), LeaveStatus.PENDING, new BigDecimal("24.00"), "ANNUAL", "Nghỉ cưới"),
                new LeaveCalendarItem(106L, 6L, "EMP-06", "Đỗ Thị F", LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30), LeaveStatus.APPROVED, new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm")
        );
        when(loadDepartmentMonthlyLeavePort.findLeavesForEmployees(eq(List.of(1L, 2L, 3L, 4L, 5L, 6L)), eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30)), anyMap()))
                .thenReturn(sixLeaves);

        // Execute
        DepartmentMonthlyLeaveCalendarResult result = service.execute(query);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.orgUnitId());
        assertEquals("DEV-DEP", result.orgUnitCode());
        assertEquals(2026, result.year());
        assertEquals(9, result.month());
        assertEquals(6, result.totalDepartmentEmployees());
        assertEquals(6, result.totalLeaveRequests(), "Hiện đúng 6 khoảng nghỉ mô phỏng trên lịch tháng");
        assertEquals(30, result.dailySummaries().size());

        // TC-04: Hệ thống ghi lại người thực hiện, nội dung và thời điểm
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(2L) &&
                "VIEW_DEPARTMENT_LEAVE_CALENDAR".equals(log.getAction()) &&
                "leave_requests".equals(log.getTableName()) &&
                log.getRecordId().equals(10L)
        ));
    }

    @Test
    @DisplayName("TC-02: Ngoại lệ - Bốn trên năm người trong bộ phận cùng nghỉ một ngày -> Hệ thống tô cảnh báo ngày đó vì số người nghỉ vượt ngưỡng")
    void tc02_fourOutOfFiveEmployeesOnLeave_triggersWarningOnDay() {
        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(10L, 2026, 9, 0.50);

        when(authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(department));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        // 5 nhân viên trong bộ phận
        List<Employee> employees = List.of(
                createEmployee(1L, "EMP-01", "A"),
                createEmployee(2L, "EMP-02", "B"),
                createEmployee(3L, "EMP-03", "C"),
                createEmployee(4L, "EMP-04", "D"),
                createEmployee(5L, "EMP-05", "E")
        );
        when(loadEmployeePort.findActiveByOrgUnitId(10L)).thenReturn(employees);

        LocalDate criticalDate = LocalDate.of(2026, 9, 15);
        List<LeaveCalendarItem> leaves = List.of(
                new LeaveCalendarItem(101L, 1L, "EMP-01", "A", criticalDate, criticalDate, LeaveStatus.APPROVED, new BigDecimal("8.00"), "ANNUAL", "Lý do"),
                new LeaveCalendarItem(102L, 2L, "EMP-02", "B", criticalDate, criticalDate, LeaveStatus.APPROVED, new BigDecimal("8.00"), "ANNUAL", "Lý do"),
                new LeaveCalendarItem(103L, 3L, "EMP-03", "C", criticalDate, criticalDate, LeaveStatus.PENDING, new BigDecimal("8.00"), "ANNUAL", "Lý do"),
                new LeaveCalendarItem(104L, 4L, "EMP-04", "D", criticalDate, criticalDate, LeaveStatus.PENDING, new BigDecimal("8.00"), "ANNUAL", "Lý do")
        );
        when(loadDepartmentMonthlyLeavePort.findLeavesForEmployees(anyList(), any(LocalDate.class), any(LocalDate.class), anyMap()))
                .thenReturn(leaves);

        // Execute
        DepartmentMonthlyLeaveCalendarResult result = service.execute(query);

        // Assert: Ngày 15/9 có cảnh báo
        DepartmentMonthlyLeaveCalendarResult.DailyLeaveSummaryResult day15 = result.dailySummaries().stream()
                .filter(d -> d.date().equals(criticalDate))
                .findFirst()
                .orElseThrow();

        assertTrue(day15.isWarning(), "Ngày 15/9 phải được đánh dấu cảnh báo");
        assertEquals(4, day15.totalOnLeave());
        assertEquals(2, day15.approvedCount());
        assertEquals(2, day15.pendingCount());
        assertNotNull(day15.warningMessage());
        assertTrue(day15.warningMessage().contains("4/5"));
        assertEquals(1, result.warningDaysCount());
    }

    @Test
    @DisplayName("TC-03: Không có quyền - Người dùng không thuộc vai trò Quản lý nguồn lực (thiếu quyền) -> Hệ thống từ chối")
    void tc03_missingPermission_throwsPermissionDeniedException() {
        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(10L, 2026, 9, 0.50);

        when(authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.DEPARTMENT_LEAVE_READ));

        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        verify(saveAuditLogPort, never()).save(argThat(log -> "VIEW_DEPARTMENT_LEAVE_CALENDAR".equals(log.getAction())));
    }

    @Test
    @DisplayName("TC-03b: Không có quyền - Vai trò Quản lý nguồn lực nhưng tra cứu bộ phận ngoài phạm vi Data Scope ORGANIZATION_BRANCH -> Từ chối và ghi log")
    void tc03b_outOfDataScope_throwsPermissionDeniedAndLogsAccessDenied() {
        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(99L, 2026, 9, 0.50);

        when(authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));

        OrgUnit otherDept = new OrgUnit(
                new OrgUnitId(99L),
                "OTHER-DEP",
                "Phòng Ban Khác",
                OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L),
                "/1/99/",
                2,
                OrgUnitStatus.ACTIVE,
                "Bộ phận khác",
                2L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(loadOrgUnitPort.findById(new OrgUnitId(99L))).thenReturn(Optional.of(otherDept));
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        // Execute & Assert
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));

        // Kiểm tra đã ghi nhật ký lần từ chối (ACCESS_DENIED)
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(2L) &&
                "ACCESS_DENIED".equals(log.getAction()) &&
                "org_units".equals(log.getTableName()) &&
                log.getRecordId().equals(99L)
        ));
    }

    @Test
    @DisplayName("Bộ phận không tồn tại -> Ném OrgUnitNotFoundException")
    void orgUnitNotFound_throwsOrgUnitNotFoundException() {
        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(999L, 2026, 9, 0.50);

        when(authorizationService.require(PermissionCode.DEPARTMENT_LEAVE_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(999L))).thenReturn(Optional.empty());

        assertThrows(OrgUnitNotFoundException.class, () -> service.execute(query));
    }

    private Employee createEmployee(Long id, String code, String name) {
        return new Employee(
                new EmployeeId(id),
                new UserId(id + 100),
                10L,
                code,
                name,
                "DEV",
                null,
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }
}
