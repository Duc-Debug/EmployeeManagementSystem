package com.hrm.employeemanagement.application.service.report.timesheetvariance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceItem;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceQuery;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.timesheetvariance.LoadTimesheetVariancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("GetTimesheetVarianceService Unit Tests (NCL-09-CN-004)")
class GetTimesheetVarianceServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadProjectPort loadProjectPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadTimesheetVariancePort loadTimesheetVariancePort;
    private SaveAuditLogPort saveAuditLogPort;

    private GetTimesheetVarianceService service;

    private final Long USER_VT03_ID = 200L;
    private final Long ORG_UNIT_ID = 10L;
    private final Long EMPLOYEE_ID = 1L;
    private final Long PROJECT_ID = 101L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadProjectPort = mock(LoadProjectPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        loadTimesheetVariancePort = mock(LoadTimesheetVariancePort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new GetTimesheetVarianceService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadAllocationPort,
                loadTimesheetVariancePort,
                saveAuditLogPort
        );

        when(authorizationService.require(PermissionCode.TIMESHEET_VARIANCE_READ)).thenReturn(USER_VT03_ID);

        User rmUser = mock(User.class);
        when(rmUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(rmUser.getScopeOrgUnitId()).thenReturn(ORG_UNIT_ID);
        when(loadUserPort.findById(new UserId(USER_VT03_ID))).thenReturn(Optional.of(rmUser));

        OrgUnit orgUnit = mock(OrgUnit.class);
        when(orgUnit.getId()).thenReturn(new OrgUnitId(ORG_UNIT_ID));
        when(orgUnit.getUnitName()).thenReturn("Trung tâm Phần mềm");
        when(orgUnit.getTreePath()).thenReturn("/10/");
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(orgUnit));
        when(loadOrgUnitPort.findSubTree("/10/")).thenReturn(List.of(orgUnit));
        when(loadOrgUnitPort.existsInOrgUnitBranch(ORG_UNIT_ID, ORG_UNIT_ID)).thenReturn(true);
    }

    @Test
    @DisplayName("TC-01: Phân bổ 30h và thực tế duyệt 38h -> Chênh lệch dương 8h (+8h)")
    void testSuccessVarianceCalculation_PositiveVariance() {
        // Given: Tuần 35/2026, Phân bổ 30h, Thực tế duyệt 38h
        int year = 2026;
        int week = 35;

        Employee emp = mock(Employee.class);
        when(emp.getIdValue()).thenReturn(EMPLOYEE_ID);
        when(emp.getEmployeeCode()).thenReturn("EMP001");
        when(emp.getFullName()).thenReturn("Nguyen Van A");
        when(emp.getOrgUnitId()).thenReturn(ORG_UNIT_ID);
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(ORG_UNIT_ID))).thenReturn(List.of(emp));

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                1L, EMPLOYEE_ID, PROJECT_ID, YearWeek.of(year, week), BigDecimal.valueOf(30.0)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(eq(List.of(EMPLOYEE_ID)), any())).thenReturn(List.of(alloc));

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(new ProjectId(PROJECT_ID));
        when(project.getProjectName()).thenReturn("Hệ thống HRM");
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(project));

        String key = EMPLOYEE_ID + "_" + PROJECT_ID + "_" + year + "_" + week;
        when(loadTimesheetVariancePort.loadApprovedActualHours(eq(List.of(EMPLOYEE_ID)), any(), any()))
                .thenReturn(Map.of(key, BigDecimal.valueOf(38.0)));

        // When
        TimesheetVarianceQuery query = new TimesheetVarianceQuery(ORG_UNIT_ID, null, null, year, week, year, week);
        TimesheetVarianceResult result = service.execute(query);

        // Then
        assertNotNull(result);
        assertTrue(result.hasAnyActualData());
        assertEquals(1, result.items().size());

        TimesheetVarianceItem item = result.items().get(0);
        assertEquals(BigDecimal.valueOf(30.0).setScale(1), item.allocatedHours());
        assertEquals(BigDecimal.valueOf(38.0).setScale(1), item.actualApprovedHours());
        assertEquals(BigDecimal.valueOf(8.0).setScale(1), item.varianceHours());
        assertEquals("POSITIVE_VARIANCE", item.varianceStatus());
        assertTrue(item.hasActualData());

        // TC-04 Verify audit log was recorded
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Tuần chưa có giờ công nào được duyệt -> Thông báo chưa đủ dữ liệu thực tế")
    void testEmptyActualData_ReturnsWarningMessage() {
        // Given: Tuần 35/2026, Phân bổ 40h nhưng chưa có timesheet duyệt
        int year = 2026;
        int week = 35;

        Employee emp = mock(Employee.class);
        when(emp.getIdValue()).thenReturn(EMPLOYEE_ID);
        when(emp.getEmployeeCode()).thenReturn("EMP001");
        when(emp.getFullName()).thenReturn("Nguyen Van A");
        when(emp.getOrgUnitId()).thenReturn(ORG_UNIT_ID);
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(ORG_UNIT_ID))).thenReturn(List.of(emp));

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                1L, EMPLOYEE_ID, PROJECT_ID, YearWeek.of(year, week), BigDecimal.valueOf(40.0)
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(eq(List.of(EMPLOYEE_ID)), any())).thenReturn(List.of(alloc));

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(new ProjectId(PROJECT_ID));
        when(project.getProjectName()).thenReturn("Hệ thống HRM");
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(project));

        // Thực tế duyệt = 0
        when(loadTimesheetVariancePort.loadApprovedActualHours(eq(List.of(EMPLOYEE_ID)), any(), any()))
                .thenReturn(Map.of());

        // When
        TimesheetVarianceQuery query = new TimesheetVarianceQuery(ORG_UNIT_ID, null, null, year, week, year, week);
        TimesheetVarianceResult result = service.execute(query);

        // Then
        assertNotNull(result);
        assertFalse(result.hasAnyActualData());
        assertEquals("Chưa đủ dữ liệu thực tế để đối chiếu", result.message());
        assertEquals(1, result.summary().noActualDataCount());
    }

    @Test
    @DisplayName("TC-03: Người dùng ngoài phạm vi tổ chức (Data Scope) -> Từ chối truy cập và ghi log")
    void testUnauthorizedOrgUnitScope_ThrowsPermissionDenied() {
        // Given: Query bộ phận 999 ngoài phạm vi quản lý của RM
        Long unauthorizedOrgUnitId = 999L;
        when(loadOrgUnitPort.existsInOrgUnitBranch(unauthorizedOrgUnitId, ORG_UNIT_ID)).thenReturn(false);

        TimesheetVarianceQuery query = new TimesheetVarianceQuery(unauthorizedOrgUnitId, null, null, 2026, 35, 2026, 35);

        // When & Then
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-04: Giờ phân bổ = 0h, Thực tế duyệt = 20h -> variancePercentage = null (N/A), trạng thái POSITIVE_VARIANCE")
    void testVariancePercentage_WhenAllocatedIsZero_ReturnsNull() {
        // Given: Tuần 35/2026, Phân bổ 0h (hoặc không có bản ghi phân bổ), Thực tế duyệt 20h
        int year = 2026;
        int week = 35;

        Employee emp = mock(Employee.class);
        when(emp.getIdValue()).thenReturn(EMPLOYEE_ID);
        when(emp.getEmployeeCode()).thenReturn("EMP001");
        when(emp.getFullName()).thenReturn("Nguyen Van A");
        when(emp.getOrgUnitId()).thenReturn(ORG_UNIT_ID);
        when(loadEmployeePort.findActiveByOrgUnitIds(List.of(ORG_UNIT_ID))).thenReturn(List.of(emp));

        // Không có bản ghi phân bổ (allocated = 0h)
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(eq(List.of(EMPLOYEE_ID)), any())).thenReturn(List.of());

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(new ProjectId(PROJECT_ID));
        when(project.getProjectName()).thenReturn("Hệ thống HRM");
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(project));

        String key = EMPLOYEE_ID + "_" + PROJECT_ID + "_" + year + "_" + week;
        when(loadTimesheetVariancePort.loadApprovedActualHours(eq(List.of(EMPLOYEE_ID)), any(), any()))
                .thenReturn(Map.of(key, BigDecimal.valueOf(20.0)));

        // When
        TimesheetVarianceQuery query = new TimesheetVarianceQuery(ORG_UNIT_ID, null, null, year, week, year, week);
        TimesheetVarianceResult result = service.execute(query);

        // Then
        assertNotNull(result);
        assertTrue(result.hasAnyActualData());
        assertEquals(1, result.items().size());

        TimesheetVarianceItem item = result.items().get(0);
        assertEquals(BigDecimal.ZERO.setScale(1), item.allocatedHours());
        assertEquals(BigDecimal.valueOf(20.0).setScale(1), item.actualApprovedHours());
        assertEquals(BigDecimal.valueOf(20.0).setScale(1), item.varianceHours());
        org.junit.jupiter.api.Assertions.assertNull(item.variancePercentage(), "variancePercentage phải là null khi allocated = 0 để tránh chia cho 0");
        assertEquals("POSITIVE_VARIANCE", item.varianceStatus());
    }

    @Test
    @DisplayName("TC-05: Query có employeeId cụ thể -> Tải trực tiếp nhân viên đó mà không load toàn bộ danh sách bộ phận")
    void testExecute_WithSpecificEmployeeId_LoadsDirectly() {
        int year = 2026;
        int week = 35;

        Employee emp = mock(Employee.class);
        when(emp.getIdValue()).thenReturn(EMPLOYEE_ID);
        when(emp.getEmployeeCode()).thenReturn("EMP001");
        when(emp.getFullName()).thenReturn("Nguyen Van A");
        when(emp.getOrgUnitId()).thenReturn(ORG_UNIT_ID);
        when(loadEmployeePort.findById(new com.hrm.employeemanagement.domain.employee.EmployeeId(EMPLOYEE_ID))).thenReturn(Optional.of(emp));

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(eq(List.of(EMPLOYEE_ID)), any())).thenReturn(List.of());
        when(loadTimesheetVariancePort.loadApprovedActualHours(eq(List.of(EMPLOYEE_ID)), any(), any())).thenReturn(Map.of());

        // When
        TimesheetVarianceQuery query = new TimesheetVarianceQuery(ORG_UNIT_ID, EMPLOYEE_ID, null, year, week, year, week);
        TimesheetVarianceResult result = service.execute(query);

        // Then
        assertNotNull(result);
        verify(loadEmployeePort).findById(new com.hrm.employeemanagement.domain.employee.EmployeeId(EMPLOYEE_ID));
        verify(loadEmployeePort, org.mockito.Mockito.never()).findActiveByOrgUnitIds(any());
        verify(loadEmployeePort, org.mockito.Mockito.never()).findAllActive();
    }
}
