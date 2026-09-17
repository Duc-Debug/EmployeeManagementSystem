package com.hrm.employeemanagement.application.service.report.projectallocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportExport;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportQuery;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@DisplayName("GetProjectAllocationReportService Unit Tests (NCL-10-CN-006)")
class GetProjectAllocationReportServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadProjectPort loadProjectPort;
    private LoadProjectResourceDemandPort loadDemandPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadProjectRolePort loadProjectRolePort;
    private LoadProjectMemberPort loadProjectMemberPort;
    private SaveAuditLogPort saveAuditLogPort;

    private GetProjectAllocationReportService service;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadProjectPort = mock(LoadProjectPort.class);
        loadDemandPort = mock(LoadProjectResourceDemandPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        loadProjectRolePort = mock(LoadProjectRolePort.class);
        loadProjectMemberPort = mock(LoadProjectMemberPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new GetProjectAllocationReportService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadDemandPort,
                loadAllocationPort,
                loadProjectRolePort,
                loadProjectMemberPort,
                saveAuditLogPort
        );
    }

    @Test
    @DisplayName("NCL-10-CN-006-TC-01: Luồng thành công - Dự án cần 40 giờ tuần này nhưng mới phân bổ 30 giờ -> Hệ thống hiện thiếu 10 giờ")
    void testTC01_SuccessFlow_ShortageCalculation() {
        Long currentUserId = 200L;
        Long pmEmployeeId = 20L;
        Long projectId = 1L;
        int targetYear = 2026;
        int targetWeek = 38;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý Dự án");
        User pmUser = new User(new UserId(currentUserId), "pm_user", "hash", pmRole, UserStatus.ACTIVE,
                new EmployeeId(pmEmployeeId), DataScope.SELF, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));

        Project project = Project.createNew(
                "PRJ-001",
                "Hệ thống HRM Core",
                10L,
                new EmployeeId(pmEmployeeId),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(160.0),
                "Mô tả dự án",
                new UserId(currentUserId)
        );
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadProjectPort.existsManagedBy(projectId, pmEmployeeId)).thenReturn(true);

        OrgUnit orgUnit = new OrgUnit(new OrgUnitId(10L), "TT-PM", "Trung tâm phát sinh phần mềm", OrgUnitType.DEPARTMENT,
                null, "/10/", 1, OrgUnitStatus.ACTIVE, "Mô tả", pmEmployeeId, LocalDateTime.now(), LocalDateTime.now());
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(orgUnit));

        Employee pmEmp = new Employee(new EmployeeId(pmEmployeeId), new UserId(currentUserId), 10L, "NV020", "Nguyễn Văn PM",
                "Quản lý dự án", LocalDate.of(2020, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(pmEmployeeId))).thenReturn(Optional.of(pmEmp));

        // Vai trò Backend Developer
        ProjectRole backendRole = new ProjectRole(new ProjectRoleId(101L), "BACKEND_DEV", "Backend Developer", "Lập trình viên Backend");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(backendRole));

        // TC-01: Nhu cầu là 40 giờ trong tuần 38/2026
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(projectId),
                new ProjectRoleId(101L),
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(40.0)
        );
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of(demand));

        // TC-01: Mới phân bổ 30 giờ cho 1 nhân viên Backend
        Long devEmployeeId = 301L;
        WeeklyProjectAllocation alloc = WeeklyProjectAllocation.createNew(
                devEmployeeId,
                projectId,
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(75.0)
        );
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, targetYear, targetWeek, targetWeek))
                .thenReturn(List.of(alloc));

        Employee devEmp = new Employee(new EmployeeId(devEmployeeId), new UserId(300L), 10L, "NV301", "Trần Văn Dev",
                "Backend Developer", LocalDate.of(2022, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(devEmployeeId)))).thenReturn(List.of(devEmp));

        // Thực thi
        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, targetYear, targetWeek, targetYear, targetWeek);
        ProjectAllocationReportResult result = service.execute(query);

        // Kiểm tra kết quả theo TC-01
        assertNotNull(result);
        assertEquals(projectId, result.projectId());
        assertEquals("PRJ-001", result.projectCode());
        assertEquals("Hệ thống HRM Core", result.projectName());
        assertEquals("Nguyễn Văn PM", result.managerName());
        assertEquals("Trung tâm phát sinh phần mềm", result.orgUnitName());

        // Tổng hợp số giờ: Nhu cầu 40.0, Đã phân bổ 30.0, Thiếu 10.0 giờ (TC-01)
        assertEquals(new BigDecimal("40.0"), result.totalDemandHours());
        assertEquals(new BigDecimal("30.0"), result.totalAllocatedHours());
        assertEquals(new BigDecimal("10.0"), result.totalShortfallHours());
        assertEquals(1, result.shortageWeeksCount());

        // Kiểm tra Weekly Summary
        assertEquals(1, result.weeklySummaries().size());
        assertEquals("SHORTAGE", result.weeklySummaries().get(0).status());
        assertEquals(new BigDecimal("10.0"), result.weeklySummaries().get(0).shortfallHours());

        // Kiểm tra Role Breakdown
        assertEquals(1, result.roleBreakdowns().size());
        assertEquals("Backend Developer", result.roleBreakdowns().get(0).roleName());
        assertEquals(new BigDecimal("40.0"), result.roleBreakdowns().get(0).totalDemandHours());
        assertEquals(new BigDecimal("30.0"), result.roleBreakdowns().get(0).totalAllocatedHours());
        assertEquals(new BigDecimal("10.0"), result.roleBreakdowns().get(0).totalShortfallHours());

        // Kiểm tra Cảnh báo thiếu hụt nhân sự
        assertFalse(result.shortageAlerts().isEmpty());
        assertEquals(1, result.shortageAlerts().size());
        assertEquals(new BigDecimal("10.0"), result.shortageAlerts().get(0).missingHours());
        assertTrue(result.shortageAlerts().get(0).message().contains("Thiếu 10 giờ"));

        // Kiểm tra Audit Log thành công (TC-03)
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-006-TC-02: Không có quyền - PM mở báo cáo của dự án mình không phụ trách -> Bị từ chối (403 / PermissionDeniedException)")
    void testTC02_AccessDenied_PMAccessingUnmanagedProject() {
        Long currentUserId = 200L;
        Long pmEmployeeId = 20L;
        Long otherPmId = 99L;
        Long projectId = 2L;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý Dự án");
        User pmUser = new User(new UserId(currentUserId), "pm_user", "hash", pmRole, UserStatus.ACTIVE,
                new EmployeeId(pmEmployeeId), DataScope.SELF, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));

        // Dự án do PM khác (99L) quản lý
        Project project = Project.createNew(
                "PRJ-002",
                "Dự án bảo mật",
                10L,
                new EmployeeId(otherPmId),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(100.0),
                "Mô tả",
                new UserId(100L)
        );
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadProjectPort.existsManagedBy(projectId, pmEmployeeId)).thenReturn(false);

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, 2026, 38, 2026, 38);

        // Phải ném PermissionDeniedException
        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        assertTrue(ex.getMessage().contains("PROJECT_ALLOCATION_REPORT_READ"));

        // Kiểm tra đã ghi nhật ký kiểm toán từ chối truy cập (TC-03)
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-006-TC-02: Không có quyền - ORGANIZATION_BRANCH scope truy cập dự án ngoài nhánh -> Bị từ chối")
    void testTC02_AccessDenied_OrgBranchScopeOutsideBranch() {
        Long currentUserId = 300L;
        Long projectId = 3L;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý Nguồn lực");
        User rmUser = new User(new UserId(currentUserId), "rm_user", "hash", rmRole, UserStatus.ACTIVE,
                null, DataScope.ORGANIZATION_BRANCH, 10L, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(rmUser));

        Project project = Project.createNew(
                "PRJ-003",
                "Dự án phòng ban khác",
                99L,
                new EmployeeId(50L),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(100.0),
                "Mô tả",
                new UserId(100L)
        );
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadProjectPort.existsInOrgUnitBranch(projectId, 10L)).thenReturn(false);

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, 2026, 38, 2026, 38);
        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-006-TC-03: Lưu lịch sử - Ban Giám đốc (VT-01) COMPANY scope xem dự án bất kỳ thành công và ghi Audit Log")
    void testTC03_AuditLog_DirectorAccessCompanyScope() {
        Long currentUserId = 100L;
        Long projectId = 1L;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        User directorUser = new User(new UserId(currentUserId), "director", "hash", directorRole, UserStatus.ACTIVE,
                null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(directorUser));

        Project project = Project.createNew(
                "PRJ-001",
                "Hệ thống HRM Core",
                10L,
                new EmployeeId(20L),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(160.0),
                "Mô tả",
                new UserId(currentUserId)
        );
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findAll()).thenReturn(List.of());
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, 2026, 38, 38)).thenReturn(List.of());

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, 2026, 38, 2026, 38);
        ProjectAllocationReportResult result = assertDoesNotThrow(() -> service.execute(query));

        assertNotNull(result);
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Xuất CSV báo cáo phân bổ dự án thành công")
    void testExportCsv_Success() {
        Long currentUserId = 100L;
        Long projectId = 1L;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        User directorUser = new User(new UserId(currentUserId), "director", "hash", directorRole, UserStatus.ACTIVE,
                null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(directorUser));

        Project project = Project.createNew(
                "PRJ-001",
                "Hệ thống HRM Core",
                10L,
                new EmployeeId(20L),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(160.0),
                "Mô tả",
                new UserId(currentUserId)
        );
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findAll()).thenReturn(List.of());
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, 2026, 38, 38)).thenReturn(List.of());

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, 2026, 38, 2026, 38);
        ProjectAllocationReportExport export = service.export(query);

        assertNotNull(export);
        assertTrue(export.filename().contains("PRJ-001"));
        assertTrue(export.content().length > 0);
    }

    @Test
    @DisplayName("Ngoại lệ khi không tìm thấy dự án")
    void testProjectNotFoundException() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        User directorUser = new User(new UserId(currentUserId), "director", "hash", directorRole, UserStatus.ACTIVE,
                null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(directorUser));

        when(loadProjectPort.findById(new ProjectId(999L))).thenReturn(Optional.empty());

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(999L, 2026, 38, 2026, 38);
        assertThrows(ProjectNotFoundException.class, () -> service.execute(query));
    }
}
