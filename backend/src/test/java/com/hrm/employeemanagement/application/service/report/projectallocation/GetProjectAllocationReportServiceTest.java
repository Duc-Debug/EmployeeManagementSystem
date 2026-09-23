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
import com.hrm.employeemanagement.application.dto.report.projectallocation.RoleAllocationBreakdownItem;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
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
                101L,
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

    @Test
    @DisplayName("Không tự ý gán nhân sự không khớp vai trò (unmatched role) vào role đầu tiên trong catalog")
    void shouldNotAssignUnmatchedEmployeeToFirstProjectRole() {
        Long currentUserId = 100L;
        Long projectId = 1L;
        int targetYear = 2026;
        int targetWeek = 38;

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

        // Catalog có Backend Developer và Frontend Developer
        ProjectRole backendRole = new ProjectRole(new ProjectRoleId(1L), "BACKEND_DEV", "Backend Developer", "Backend");
        ProjectRole frontendRole = new ProjectRole(new ProjectRoleId(2L), "FRONTEND_DEV", "Frontend Developer", "Frontend");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(backendRole, frontendRole));

        // Nhu cầu dự án cần Backend Developer 40h
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(projectId),
                new ProjectRoleId(1L),
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(40.0)
        );
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of(demand));

        // Phân bổ nhân sự có professionalRole = "DevOps Engineer" (không khớp backend hay frontend)
        Long devOpsEmployeeId = 401L;
        WeeklyProjectAllocation alloc = WeeklyProjectAllocation.createNew(
                devOpsEmployeeId,
                projectId,
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(75.0)
        );
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, targetYear, targetWeek, targetWeek))
                .thenReturn(List.of(alloc));

        Employee devOpsEmp = new Employee(new EmployeeId(devOpsEmployeeId), new UserId(400L), 10L, "NV401", "Phạm Văn DevOps",
                "DevOps Engineer", LocalDate.of(2022, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(devOpsEmployeeId)))).thenReturn(List.of(devOpsEmp));

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, targetYear, targetWeek, targetYear, targetWeek);
        ProjectAllocationReportResult result = service.execute(query);

        assertNotNull(result);
        // Backend Developer không được tự ý cộng 30h của DevOps vào
        RoleAllocationBreakdownItem backendBreakdown = result.roleBreakdowns().stream()
                .filter(r -> r.roleId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("40.0"), backendBreakdown.totalDemandHours());
        assertEquals(new BigDecimal("0.0"), backendBreakdown.totalAllocatedHours(), "Không được gán giờ của DevOps vào Backend");
        assertEquals(new BigDecimal("40.0"), backendBreakdown.totalShortfallHours());
    }

    @Test
    @DisplayName("Validation: Từ chối khi projectId null")
    void shouldRejectNullProjectId() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(null, 2026, 38, 2026, 38));
    }

    @Test
    @DisplayName("Validation: Từ chối khi chỉ truyền fromYear hoặc fromWeek")
    void shouldRejectPartialFromWeekParameters() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(1L, 2026, null, 2026, 38));
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(1L, null, 38, 2026, 38));
    }

    @Test
    @DisplayName("Validation: Từ chối khi chỉ truyền toYear hoặc toWeek")
    void shouldRejectPartialToWeekParameters() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(1L, 2026, 38, 2026, null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(1L, 2026, 38, null, 38));
    }

    @Test
    @DisplayName("Validation: Từ chối khi thời gian kết thúc trước thời gian bắt đầu")
    void shouldRejectEndWeekBeforeStartWeek() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectAllocationReportQuery(1L, 2026, 40, 2026, 38));
    }

    @Test
    @DisplayName("Validation: Từ chối khi khoảng thời gian vượt quá 104 tuần")
    void shouldRejectQueryExceeding104Weeks() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ProjectAllocationReportQuery(1L, 2024, 1, 2026, 52));
        assertTrue(ex.getMessage().contains("104 tuần"));
    }

    @Test
    @DisplayName("Regression: Employee.professionalRole != ProjectRole ('Senior Backend Developer' -> DEV) - Khớp chính xác vai trò và tính đúng giờ phân bổ")
    void testRegression_EmployeeProfessionalRoleNotEqualProjectRole_CalculatesCorrectly() {
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

        // Catalog có vai trò chuẩn DEV: "Lập trình (Developer)" và TEST: "Kiểm thử (Tester / QA)"
        ProjectRole devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Lập trình (Developer)", "Phát triển");
        ProjectRole testRole = new ProjectRole(new ProjectRoleId(2L), "TEST", "Kiểm thử (Tester / QA)", "Kiểm thử");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(devRole, testRole));

        // Nhu cầu dự án cần DEV 40h
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(projectId),
                new ProjectRoleId(1L),
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(40.0)
        );
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of(demand));

        // Phân bổ nhân sự có professionalRole = "Senior Backend Developer" nhưng projectRoleId = 1L (DEV)
        Long devEmployeeId = 501L;
        WeeklyProjectAllocation alloc = WeeklyProjectAllocation.createNew(
                devEmployeeId,
                projectId,
                1L,
                YearWeek.of(targetYear, targetWeek),
                BigDecimal.valueOf(40.0),
                BigDecimal.valueOf(100.0)
        );
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, targetYear, targetWeek, targetWeek))
                .thenReturn(List.of(alloc));

        Employee devEmp = new Employee(new EmployeeId(devEmployeeId), new UserId(500L), 10L, "NV501", "Nguyễn Văn Backend",
                "Senior Backend Developer", LocalDate.of(2022, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(devEmployeeId)))).thenReturn(List.of(devEmp));

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, targetYear, targetWeek, targetYear, targetWeek);
        ProjectAllocationReportResult result = service.execute(query);

        assertNotNull(result);
        assertEquals(new BigDecimal("40.0"), result.totalDemandHours());
        assertEquals(new BigDecimal("40.0"), result.totalAllocatedHours());
        assertEquals(new BigDecimal("0.0"), result.totalShortfallHours());
        assertEquals(new BigDecimal("100.0"), result.fulfillmentRate());

        RoleAllocationBreakdownItem devBreakdown = result.roleBreakdowns().stream()
                .filter(r -> r.roleId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("40.0"), devBreakdown.totalDemandHours());
        assertEquals(new BigDecimal("40.0"), devBreakdown.totalAllocatedHours(), "Phân bổ với projectRoleId = 1L phải được tính vào vai trò DEV");
        assertEquals(new BigDecimal("0.0"), devBreakdown.totalShortfallHours());
        assertEquals(new BigDecimal("100.0"), devBreakdown.fulfillmentRate());
        assertEquals(1, devBreakdown.weeklyRoleMetrics().get(0).allocatedMembers().size());
        assertEquals("Nguyễn Văn Backend", devBreakdown.weeklyRoleMetrics().get(0).allocatedMembers().get(0).fullName());
    }

    @Test
    @DisplayName("Regression: Tech Lead và QA Automation được phân bổ chuẩn xác vào vai trò DEV và TEST tương ứng theo projectRoleId")
    void testRegression_MultipleRoles_MappedAccurately() {
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

        ProjectRole devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Lập trình (Developer)", "Phát triển");
        ProjectRole testRole = new ProjectRole(new ProjectRoleId(2L), "TEST", "Kiểm thử (Tester / QA)", "Kiểm thử");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(devRole, testRole));

        ProjectResourceDemand devDemand = ProjectResourceDemand.createNew(new ProjectId(projectId), new ProjectRoleId(1L), YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(40.0));
        ProjectResourceDemand testDemand = ProjectResourceDemand.createNew(new ProjectId(projectId), new ProjectRoleId(2L), YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(20.0));
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of(devDemand, testDemand));

        Long techLeadEmpId = 601L;
        Long qaEmpId = 602L;
        WeeklyProjectAllocation alloc1 = WeeklyProjectAllocation.createNew(techLeadEmpId, projectId, 1L, YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(40.0));
        WeeklyProjectAllocation alloc2 = WeeklyProjectAllocation.createNew(qaEmpId, projectId, 2L, YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(20.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, targetYear, targetWeek, targetWeek))
                .thenReturn(List.of(alloc1, alloc2));

        Employee techLead = new Employee(new EmployeeId(techLeadEmpId), new UserId(601L), 10L, "NV601", "Trần Tech Lead",
                "Tech Lead", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        Employee qa = new Employee(new EmployeeId(qaEmpId), new UserId(602L), 10L, "NV602", "Lê QA Automation",
                "QA Automation Specialist", LocalDate.of(2021, 5, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(techLeadEmpId), new EmployeeId(qaEmpId))))
                .thenReturn(List.of(techLead, qa));

        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, targetYear, targetWeek, targetYear, targetWeek);
        ProjectAllocationReportResult result = service.execute(query);

        assertNotNull(result);
        assertEquals(new BigDecimal("60.0"), result.totalDemandHours());
        assertEquals(new BigDecimal("60.0"), result.totalAllocatedHours());
        assertEquals(new BigDecimal("0.0"), result.totalShortfallHours());
        assertEquals(new BigDecimal("100.0"), result.fulfillmentRate());

        RoleAllocationBreakdownItem devItem = result.roleBreakdowns().stream().filter(r -> r.roleId().equals(1L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("40.0"), devItem.totalAllocatedHours());
        assertEquals(new BigDecimal("0.0"), devItem.totalShortfallHours());

        RoleAllocationBreakdownItem testItem = result.roleBreakdowns().stream().filter(r -> r.roleId().equals(2L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20.0"), testItem.totalAllocatedHours());
        assertEquals(new BigDecimal("0.0"), testItem.totalShortfallHours());
    }

    @Test
    @DisplayName("Regression: 1 nhân sự phân bổ 2 dự án với 2 vai trò khác nhau (Project A: DEV, Project B: TEST) được tính đúng từng báo cáo")
    void testRegression_OneEmployee_TwoProjects_TwoDifferentRoles() {
        Long currentUserId = 100L;
        Long projectIdA = 10L;
        Long projectIdB = 20L;
        int targetYear = 2026;
        int targetWeek = 38;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        User directorUser = new User(new UserId(currentUserId), "director", "hash", directorRole, UserStatus.ACTIVE,
                null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(directorUser));

        Project projectA = Project.createNew("PRJ-A", "Dự án A", 10L, new EmployeeId(20L), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20), BigDecimal.valueOf(160.0), "Mô tả", new UserId(currentUserId));
        Project projectB = Project.createNew("PRJ-B", "Dự án B", 10L, new EmployeeId(20L), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20), BigDecimal.valueOf(160.0), "Mô tả", new UserId(currentUserId));
        when(loadProjectPort.findById(new ProjectId(projectIdA))).thenReturn(Optional.of(projectA));
        when(loadProjectPort.findById(new ProjectId(projectIdB))).thenReturn(Optional.of(projectB));

        ProjectRole devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Lập trình", "Dev");
        ProjectRole testRole = new ProjectRole(new ProjectRoleId(2L), "TEST", "Kiểm thử", "Test");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(devRole, testRole));

        // Demand
        ProjectResourceDemand demandA = ProjectResourceDemand.createNew(new ProjectId(projectIdA), new ProjectRoleId(1L), YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(30.0));
        ProjectResourceDemand demandB = ProjectResourceDemand.createNew(new ProjectId(projectIdB), new ProjectRoleId(2L), YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(10.0));
        when(loadDemandPort.findByProjectId(new ProjectId(projectIdA))).thenReturn(List.of(demandA));
        when(loadDemandPort.findByProjectId(new ProjectId(projectIdB))).thenReturn(List.of(demandB));

        // Employee A: professionalRole = "Software Engineer"
        Long employeeId = 701L;
        Employee emp = new Employee(new EmployeeId(employeeId), new UserId(701L), 10L, "NV701", "Nguyễn Văn Đa Năng",
                "Software Engineer", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(employeeId)))).thenReturn(List.of(emp));

        // Allocation Project A: 30h with projectRoleId = 1L (DEV)
        WeeklyProjectAllocation allocA = WeeklyProjectAllocation.createNew(employeeId, projectIdA, 1L, YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(30.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectIdA, targetYear, targetWeek, targetWeek)).thenReturn(List.of(allocA));

        // Allocation Project B: 10h with projectRoleId = 2L (TEST)
        WeeklyProjectAllocation allocB = WeeklyProjectAllocation.createNew(employeeId, projectIdB, 2L, YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(10.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectIdB, targetYear, targetWeek, targetWeek)).thenReturn(List.of(allocB));

        // Query Project A Report
        ProjectAllocationReportResult resultA = service.execute(new ProjectAllocationReportQuery(projectIdA, targetYear, targetWeek, targetYear, targetWeek));
        assertEquals(new BigDecimal("30.0"), resultA.totalAllocatedHours());
        RoleAllocationBreakdownItem devItem = resultA.roleBreakdowns().stream().filter(r -> r.roleId().equals(1L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("30.0"), devItem.totalAllocatedHours());

        // Query Project B Report
        ProjectAllocationReportResult resultB = service.execute(new ProjectAllocationReportQuery(projectIdB, targetYear, targetWeek, targetYear, targetWeek));
        assertEquals(new BigDecimal("10.0"), resultB.totalAllocatedHours());
        RoleAllocationBreakdownItem testItem = resultB.roleBreakdowns().stream().filter(r -> r.roleId().equals(2L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.0"), testItem.totalAllocatedHours());
    }

    @Test
    @DisplayName("Regression: Phân bổ dữ liệu cũ (projectRoleId = null) được gom vào nhóm 'Vai trò khác / Chưa phân loại'")
    void testRegression_LegacyAllocation_NullProjectRoleId_GroupedAsUnclassified() {
        Long currentUserId = 100L;
        Long projectId = 1L;
        int targetYear = 2026;
        int targetWeek = 38;

        when(authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ)).thenReturn(currentUserId);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        User directorUser = new User(new UserId(currentUserId), "director", "hash", directorRole, UserStatus.ACTIVE,
                null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(directorUser));

        Project project = Project.createNew("PRJ-001", "Hệ thống HRM", 10L, new EmployeeId(20L), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20), BigDecimal.valueOf(160.0), "Mô tả", new UserId(currentUserId));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));

        ProjectRole devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Lập trình", "Dev");
        when(loadProjectRolePort.findAll()).thenReturn(List.of(devRole));
        when(loadDemandPort.findByProjectId(new ProjectId(projectId))).thenReturn(List.of());

        // Allocation có projectRoleId == null
        Long empId = 801L;
        WeeklyProjectAllocation alloc = WeeklyProjectAllocation.createNew(empId, projectId, null, YearWeek.of(targetYear, targetWeek), BigDecimal.valueOf(25.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, targetYear, targetWeek, targetWeek)).thenReturn(List.of(alloc));

        Employee emp = new Employee(new EmployeeId(empId), new UserId(801L), 10L, "NV801", "Nhân viên Cũ", "Lập trình", LocalDate.of(2020, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(empId)))).thenReturn(List.of(emp));

        ProjectAllocationReportResult result = service.execute(new ProjectAllocationReportQuery(projectId, targetYear, targetWeek, targetYear, targetWeek));

        assertNotNull(result);
        assertEquals(new BigDecimal("25.0"), result.totalAllocatedHours());

        RoleAllocationBreakdownItem unclassified = result.roleBreakdowns().stream()
                .filter(r -> r.roleId().equals(0L))
                .findFirst()
                .orElseThrow();
        assertEquals("OTHER", unclassified.roleCode());
        assertEquals("Vai trò khác / Chưa phân loại", unclassified.roleName());
        assertEquals(new BigDecimal("25.0"), unclassified.totalAllocatedHours());
    }
}
