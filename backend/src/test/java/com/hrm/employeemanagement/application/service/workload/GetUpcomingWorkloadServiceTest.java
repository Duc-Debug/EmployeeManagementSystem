package com.hrm.employeemanagement.application.service.workload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hrm.employeemanagement.application.dto.workload.GetUpcomingWorkloadQuery;
import com.hrm.employeemanagement.application.dto.workload.UpcomingWorkloadResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetUpcomingWorkloadService Application Tests (NCL-13-CN-004)")
class GetUpcomingWorkloadServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadProjectRolePort loadProjectRolePort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;

    @Mock
    private LoadCapacityThresholdPort loadCapacityThresholdPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private GetUpcomingWorkloadService service;

    private User employeeUser;
    private Employee employee;
    private OrgUnit orgUnit;

    @BeforeEach
    void setUp() {
        service = new GetUpcomingWorkloadService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadProjectRolePort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                loadCapacityThresholdPort,
                saveAuditLogPort
        );

        Role employeeRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        employeeUser = new User(new UserId(100L), "dev01", "hash", employeeRole, UserStatus.ACTIVE, new EmployeeId(10L), DataScope.SELF, null, 0L);

        employee = new Employee(
                new EmployeeId(10L),
                new UserId(100L),
                1L,
                "EMP010",
                "Nguyen Van Dev",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        orgUnit = mock(OrgUnit.class);
        when(orgUnit.getUnitName()).thenReturn("Phòng Phát Triển Phần Mềm");
        when(loadUserPort.findById(any(UserId.class))).thenReturn(Optional.of(employeeUser));
    }

    @Test
    @DisplayName("NCL-13-CN-004-TC-01: Luồng thành công - Lấy biểu đồ khối lượng công việc 8 tuần tới")
    void testGetMyUpcomingWorkload_Success() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        LocalDate now = LocalDate.now();
        int currentYear = now.get(IsoFields.WEEK_BASED_YEAR);
        int currentWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(new ProjectId(50L));
        when(project.getProjectName()).thenReturn("Dự án ERP Doanh nghiệp");
        when(project.getProjectCode()).thenReturn("PRJ-050");

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                1L, 10L, 50L, null, YearWeek.of(currentYear, currentWeek), BigDecimal.valueOf(30.0), BigDecimal.valueOf(75.0), 0L
        );

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(alloc));
        when(loadProjectPort.findAllById(anyList())).thenReturn(List.of(project));

        UpcomingWorkloadResult result = service.getMyUpcomingWorkload(currentYear, currentWeek, 8);

        assertNotNull(result);
        assertEquals(10L, result.employeeId());
        assertEquals("Nguyen Van Dev", result.employeeName());
        assertEquals(8, result.durationWeeks());
        assertEquals(8, result.weeklyWorkloads().size());

        // Tuần 1: 30h phân bổ / 40h chuẩn = 75% (NORMAL)
        var week1 = result.weeklyWorkloads().get(0);
        assertEquals(currentYear, week1.year());
        assertEquals(currentWeek, week1.weekNumber());
        assertEquals(BigDecimal.valueOf(40.0).setScale(1), week1.netAvailableHours());
        assertEquals(BigDecimal.valueOf(30.0).setScale(1), week1.totalAllocatedHours());
        assertEquals(BigDecimal.valueOf(75.0).setScale(1), week1.utilizationPercentage());
        assertEquals("NORMAL", week1.status());
        assertEquals(1, week1.projectAllocations().size());
        assertEquals("Dự án ERP Doanh nghiệp", week1.projectAllocations().get(0).projectName());

        // Kiểm tra audit log
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-13-CN-004-TC-02: Ngoại lệ - Tuần vượt ngưỡng quá tải (>100%) hiển thị màu cảnh báo và số giờ vượt")
    void testGetUpcomingWorkload_OverloadedWeek() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(employee));
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(employeeUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        int fromYear = 2026;
        int fromWeek = 40;

        WeeklyProjectAllocation allocOverload = new WeeklyProjectAllocation(
                1L, 10L, 50L, null, YearWeek.of(fromYear, fromWeek), BigDecimal.valueOf(48.0), BigDecimal.valueOf(120.0), 0L
        );
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(new ProjectId(50L));
        when(project.getProjectName()).thenReturn("Dự án A");
        when(project.getProjectCode()).thenReturn("PRJ-050");

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(allocOverload));
        when(loadProjectPort.findAllById(anyList())).thenReturn(List.of(project));

        GetUpcomingWorkloadQuery query = new GetUpcomingWorkloadQuery(10L, fromYear, fromWeek, 8);
        UpcomingWorkloadResult result = service.getUpcomingWorkload(query);

        assertNotNull(result);
        var week1 = result.weeklyWorkloads().get(0);
        assertEquals("OVERLOADED", week1.status());
        assertEquals(BigDecimal.valueOf(120.0).setScale(1), week1.utilizationPercentage());
        assertEquals(BigDecimal.valueOf(8.0).setScale(1), week1.overloadHours());
        assertEquals(1, result.summary().overloadedWeeksCount());
    }

    @Test
    @DisplayName("NCL-13-CN-004-TC-03: Không có quyền - Người dùng SELF cố ý xem khối lượng của người khác bị chặn và ghi Audit Log")
    void testGetUpcomingWorkload_DataScopeViolation_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);

        Employee otherEmployee = new Employee(
                new EmployeeId(20L), new UserId(200L), 2L, "EMP020", "Other Dev", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findById(new EmployeeId(20L))).thenReturn(Optional.of(otherEmployee));
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(employeeUser));

        GetUpcomingWorkloadQuery query = new GetUpcomingWorkloadQuery(20L, 2026, 40, 8);

        assertThrows(PermissionDeniedException.class, () -> service.getUpcomingWorkload(query));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(captor.capture());
        assertEquals("ACCESS_DENIED_EMPLOYEE_WORKLOAD", captor.getValue().getAction());
    }

    @Test
    @DisplayName("NCL-13-CN-004-TC-04: Lưu nhật ký kiểm toán MY_WORKLOAD_VIEWED khi xem thành công")
    void testGetMyUpcomingWorkload_RecordsAuditLog() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        service.getMyUpcomingWorkload(2026, 40, 8);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(captor.capture());
        assertEquals("MY_WORKLOAD_VIEWED", captor.getValue().getAction());
        assertEquals(10L, captor.getValue().getRecordId());
    }

    @Test
    @DisplayName("Tuần nhàn rỗi (< 70% tải) được đánh dấu là IDLE")
    void testGetUpcomingWorkload_IdleWeek() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        WeeklyProjectAllocation allocLow = new WeeklyProjectAllocation(
                1L, 10L, 50L, null, YearWeek.of(2026, 40), BigDecimal.valueOf(10.0), BigDecimal.valueOf(25.0), 0L
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(allocLow));
        when(loadProjectPort.findAllById(anyList())).thenReturn(Collections.emptyList());

        UpcomingWorkloadResult result = service.getMyUpcomingWorkload(2026, 40, 8);

        var week1 = result.weeklyWorkloads().get(0);
        assertEquals("IDLE", week1.status());
        assertEquals(BigDecimal.valueOf(25.0).setScale(1), week1.utilizationPercentage());
        assertTrue(result.summary().idleWeeksCount() >= 1);
    }

    @Test
    @DisplayName("NCL-13-CN-004: Người dùng không phải nhân viên chuyên môn (VT-04) bị chặn và ghi log từ chối")
    void testNonSpecialistRole_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        Role managerRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc");
        User managerUser = new User(new UserId(100L), "boss", "hash", managerRole, UserStatus.ACTIVE, new EmployeeId(10L), DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(managerUser));

        assertThrows(PermissionDeniedException.class, () -> service.getMyUpcomingWorkload(2026, 40, 8));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(captor.capture());
        assertEquals("ACCESS_DENIED_EMPLOYEE_WORKLOAD", captor.getValue().getAction());
        assertTrue(captor.getValue().getNewValue().contains("ROLE_NOT_SPECIALIST"));
    }

    @Test
    @DisplayName("ISO week boundary: W52 -> W53 -> W01")
    void testUpcomingWorkload_CrossesIsoWeek53Boundary() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        UpcomingWorkloadResult result = service.getMyUpcomingWorkload(2026, 52, 3);

        assertEquals(3, result.weeklyWorkloads().size());
        assertEquals(2026, result.weeklyWorkloads().get(0).year());
        assertEquals(52, result.weeklyWorkloads().get(0).weekNumber());

        assertEquals(2026, result.weeklyWorkloads().get(1).year());
        assertEquals(53, result.weeklyWorkloads().get(1).weekNumber());

        assertEquals(2027, result.weeklyWorkloads().get(2).year());
        assertEquals(1, result.weeklyWorkloads().get(2).weekNumber());
    }

    @Test
    @DisplayName("Custom Threshold: Ngưỡng quá tải cấu hình 110% thì 105% tải được đánh giá là NORMAL")
    void testUpcomingWorkload_CustomThreshold_Overload110Percent() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        CapacityThresholdConfig config = mock(CapacityThresholdConfig.class);
        when(config.getOverloadThreshold()).thenReturn(BigDecimal.valueOf(110.0));
        when(config.getIdleThreshold()).thenReturn(BigDecimal.valueOf(70.0));
        when(loadCapacityThresholdPort.findByScope(eq(CapacityThresholdScope.ORG_UNIT), eq(1L)))
                .thenReturn(Optional.of(config));

        // 42h / 40h standard = 105%
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                1L, 10L, 50L, null, YearWeek.of(2026, 40), BigDecimal.valueOf(42.0), BigDecimal.valueOf(105.0), 0L
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList())).thenReturn(List.of(alloc));
        when(loadProjectPort.findAllById(anyList())).thenReturn(Collections.emptyList());

        UpcomingWorkloadResult result = service.getMyUpcomingWorkload(2026, 40, 1);

        var week1 = result.weeklyWorkloads().get(0);
        assertEquals("NORMAL", week1.status());
        assertEquals(BigDecimal.valueOf(105.0).setScale(1), week1.utilizationPercentage());
        assertEquals(BigDecimal.valueOf(110.0).setScale(1), result.effectiveOverloadThreshold());
        assertEquals(0, result.summary().overloadedWeeksCount());
    }

    @Test
    @DisplayName("ProjectRole query failure propagates Exception without being swallowed silently")
    void testUpcomingWorkload_ProjectRoleFailure_PropagatesException() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_READ)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(orgUnit));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Map.of());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(anyList(), anyList())).thenReturn(Collections.emptyList());

        when(loadProjectRolePort.findAll()).thenThrow(new RuntimeException("DB project role connection error"));

        assertThrows(RuntimeException.class, () -> service.getMyUpcomingWorkload(2026, 40, 8));
    }
}
