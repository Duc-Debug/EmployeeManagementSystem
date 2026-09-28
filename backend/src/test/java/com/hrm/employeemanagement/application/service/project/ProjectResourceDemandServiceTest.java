package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectDateNotConfiguredException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectResourceDemandService Application Tests")
class ProjectResourceDemandServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long PROJECT_ID = 100L;
    private static final Long ROLE_ID = 4L;
    private static final Long PM_EMPLOYEE_ID = 20L;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadProjectRolePort loadProjectRolePort;

    @Mock
    private LoadProjectResourceDemandPort loadDemandPort;

    @Mock
    private SaveProjectResourceDemandPort saveDemandPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    private ProjectResourceDemandService service;

    @BeforeEach
    void setUp() {
        service = new ProjectResourceDemandService(
                loadProjectPort,
                loadProjectRolePort,
                loadDemandPort,
                saveDemandPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    @Test
    @DisplayName("TC-01: Ước lượng nhu cầu nhân sự 8 tuần thành công (Happy Path)")
    void testEstimateDemand_TC01_Success() {
        // Given: Dự án 8 tuần từ 05/10/2026 đến 29/11/2026, quy mô 200 giờ
        Project project = createActiveProject(
                PROJECT_ID,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                new BigDecimal("200.00"));
        ProjectRole role = new ProjectRole(new ProjectRoleId(ROLE_ID), "DEV", "Lập trình viên (Developer)", "Mô tả");

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findById(new ProjectRoleId(ROLE_ID))).thenReturn(Optional.of(role));
        when(loadProjectRolePort.findAll()).thenReturn(List.of(role));
        when(loadDemandPort.findByProjectIdAndRoleId(project.getId(), role.getId())).thenReturn(Collections.emptyList());

        // Giả lập lưu thành công và trả về 8 bản ghi
        when(saveDemandPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loadDemandPort.findByProjectId(project.getId())).thenAnswer(invocation -> {
            return List.of(
                    createDemand(1L, 2026, 41, new BigDecimal("20.00")),
                    createDemand(2L, 2026, 42, new BigDecimal("20.00")),
                    createDemand(3L, 2026, 43, new BigDecimal("20.00")),
                    createDemand(4L, 2026, 44, new BigDecimal("20.00")),
                    createDemand(5L, 2026, 45, new BigDecimal("20.00")),
                    createDemand(6L, 2026, 46, new BigDecimal("20.00")),
                    createDemand(7L, 2026, 47, new BigDecimal("20.00")),
                    createDemand(8L, 2026, 48, new BigDecimal("20.00")));
        });

        // When: Nhập 20h/tuần
        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("20.00"));
        ProjectResourceDemandSummaryResult result = service.estimateDemand(command);

        // Then: Bảng nhu cầu gồm đúng 8 tuần, mỗi tuần 20h, tổng 160h, không vượt quy mô (160h <= 200h)
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(PROJECT_ID);
        assertThat(result.totalDemandHours()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(result.projectEstimatedHours()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.exceedsEstimatedHours()).isFalse();
        assertThat(result.warningMessage()).isNull();
        assertThat(result.demandsByRole()).hasSize(1);
        assertThat(result.demandsByRole().get(0).weeklyDemands()).hasSize(8);

        // TC-04: Đã ghi nhận Audit Log
        verify(saveDemandPort).saveAll(anyList());
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Cảnh báo khi tổng nhu cầu nhân sự vượt quá quy mô dự án")
    void testEstimateDemand_TC02_ExceedsEstimatedHours_Warning() {
        // Given: Dự án 8 tuần nhưng quy mô chỉ có 100 giờ
        Project project = createActiveProject(
                PROJECT_ID,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                new BigDecimal("100.00"));
        ProjectRole role = new ProjectRole(new ProjectRoleId(ROLE_ID), "DEV", "Lập trình viên (Developer)", "Mô tả");

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findById(new ProjectRoleId(ROLE_ID))).thenReturn(Optional.of(role));
        when(loadProjectRolePort.findAll()).thenReturn(List.of(role));
        when(loadDemandPort.findByProjectIdAndRoleId(project.getId(), role.getId())).thenReturn(Collections.emptyList());

        // Trả về 8 tuần x 20h = 160h
        when(loadDemandPort.findByProjectId(project.getId())).thenReturn(List.of(
                createDemand(1L, 2026, 41, new BigDecimal("20.00")),
                createDemand(2L, 2026, 42, new BigDecimal("20.00")),
                createDemand(3L, 2026, 43, new BigDecimal("20.00")),
                createDemand(4L, 2026, 44, new BigDecimal("20.00")),
                createDemand(5L, 2026, 45, new BigDecimal("20.00")),
                createDemand(6L, 2026, 46, new BigDecimal("20.00")),
                createDemand(7L, 2026, 47, new BigDecimal("20.00")),
                createDemand(8L, 2026, 48, new BigDecimal("20.00"))));

        // When
        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("20.00"));
        ProjectResourceDemandSummaryResult result = service.estimateDemand(command);

        // Then: Dữ liệu vẫn được lưu nhưng cờ cảnh báo bật lên
        assertThat(result.totalDemandHours()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(result.projectEstimatedHours()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.exceedsEstimatedHours()).isTrue();
        assertThat(result.warningMessage()).contains("Nhu cầu nhân sự vượt quá quy mô dự án");

        verify(saveDemandPort).saveAll(anyList());
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-03: Từ chối truy cập và ghi log kiểm toán khi người dùng không phụ trách dự án (Scope SELF)")
    void testEstimateDemand_TC03_OutsideDataScope_Forbidden() {
        // Given: User có scope SELF nhưng không phải PM của dự án
        User pmUser = createPmUser(CURRENT_USER_ID, PM_EMPLOYEE_ID);
        Project otherProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án của PM khác",
                10L,
                new EmployeeId(999L), // PM khác
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                new BigDecimal("200.00"),
                null,
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(otherProject));
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createEmployee(PM_EMPLOYEE_ID)));

        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("20.00"));

        // When & Then
        assertThatThrownBy(() -> service.estimateDemand(command))
                .isInstanceOf(PermissionDeniedException.class);

        // Đã ghi log từ chối truy cập độc lập
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
        verify(saveDemandPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Báo lỗi khi dự án chưa cấu hình ngày bắt đầu hoặc ngày kết thúc")
    void testEstimateDemand_MissingDates_ThrowsException() {
        Project projectNoDates = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-NODATE",
                "Dự án chưa có ngày",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                null, // thiếu ngày
                null,
                BigDecimal.ZERO,
                null,
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(projectNoDates));

        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.estimateDemand(command))
                .isInstanceOf(ProjectDateNotConfiguredException.class)
                .hasMessageContaining("chưa có ngày bắt đầu hoặc ngày kết thúc");
    }

    @Test
    @DisplayName("Báo lỗi khi dự án đã bị đóng hoặc vô hiệu hóa (INACTIVE)")
    void testEstimateDemand_InactiveProject_ThrowsException() {
        Project inactiveProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-INACTIVE",
                "Dự án đã đóng",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                BigDecimal.valueOf(100),
                null,
                ProjectStatus.INACTIVE, // Đã đóng
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(inactiveProject));

        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.estimateDemand(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("hoạt động");
    }

    @Test
    @DisplayName("Báo lỗi khi vai trò chuyên môn không tồn tại")
    void testEstimateDemand_RoleNotFound_ThrowsException() {
        Project project = createActiveProject(
                PROJECT_ID,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                new BigDecimal("200.00"));

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findById(new ProjectRoleId(999L))).thenReturn(Optional.empty());

        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, 999L, new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.estimateDemand(command))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    @DisplayName("Tự động dọn dẹp (delete) các demand cũ nằm ngoài thời gian dự án khi dự án bị rút ngắn")
    void testEstimateDemand_ShortenedProjectDates_DeletesStaleWeeks() {
        // Given: Dự án bị rút ngắn chỉ còn 4 tuần (từ tuần 41 đến tuần 44)
        Project project = createActiveProject(
                PROJECT_ID,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 1), // 4 tuần: 41, 42, 43, 44
                new BigDecimal("100.00"));
        ProjectRole role = new ProjectRole(new ProjectRoleId(ROLE_ID), "DEV", "Lập trình viên (Developer)", "Mô tả");

        // Trước đó trong DB đã có 6 tuần (41, 42, 43, 44, 45, 46)
        List<ProjectResourceDemand> existingDemands = List.of(
                createDemand(1L, 2026, 41, new BigDecimal("20.00")),
                createDemand(2L, 2026, 42, new BigDecimal("20.00")),
                createDemand(3L, 2026, 43, new BigDecimal("20.00")),
                createDemand(4L, 2026, 44, new BigDecimal("20.00")),
                createDemand(5L, 2026, 45, new BigDecimal("20.00")), // stale week
                createDemand(6L, 2026, 46, new BigDecimal("20.00"))  // stale week
        );

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findById(new ProjectRoleId(ROLE_ID))).thenReturn(Optional.of(role));
        when(loadProjectRolePort.findAll()).thenReturn(List.of(role));
        when(loadDemandPort.findByProjectIdAndRoleId(project.getId(), role.getId())).thenReturn(existingDemands);
        when(loadDemandPort.findByProjectId(project.getId())).thenReturn(existingDemands);

        // When: PM ước lượng lại với 15h/tuần
        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                PROJECT_ID, ROLE_ID, new BigDecimal("15.00"));
        ProjectResourceDemandSummaryResult result = service.estimateDemand(command);

        // Then:
        // 1. Phải gọi deleteAll cho 2 tuần thừa (tuần 45 và 46)
        verify(saveDemandPort).deleteAll(anyList());
        // 2. Phải gọi saveAll cho 4 tuần hợp lệ
        verify(saveDemandPort).saveAll(anyList());
        // 3. Kết quả summary chỉ chứa đúng 4 tuần hợp lệ (tổng 60h, không bị cộng 2 tuần stale)
        assertThat(result.demandsByRole().get(0).weeklyDemands()).hasSize(4);
    }

    @Test
    @DisplayName("Xóa ước lượng nhu cầu nhân sự của vai trò thành công (Happy Path)")
    void testDeleteDemand_Success() {
        Project project = createActiveProject(
                PROJECT_ID,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                new BigDecimal("200.00"));
        ProjectRole role = new ProjectRole(new ProjectRoleId(ROLE_ID), "DEV", "Lập trình viên (Developer)", "Mô tả");

        List<ProjectResourceDemand> existingDemands = List.of(
                createDemand(1L, 2026, 41, new BigDecimal("20.00")),
                createDemand(2L, 2026, 42, new BigDecimal("20.00"))
        );

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(loadProjectRolePort.findById(new ProjectRoleId(ROLE_ID))).thenReturn(Optional.of(role));
        when(loadProjectRolePort.findAll()).thenReturn(List.of(role));
        when(loadDemandPort.findByProjectIdAndRoleId(project.getId(), role.getId())).thenReturn(existingDemands);
        when(loadDemandPort.findByProjectId(project.getId())).thenReturn(Collections.emptyList());

        ProjectResourceDemandSummaryResult result = service.deleteDemand(PROJECT_ID, ROLE_ID);

        assertThat(result).isNotNull();
        assertThat(result.demandsByRole()).isEmpty();
        assertThat(result.totalDemandHours()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(saveDemandPort).deleteAll(existingDemands);
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Báo lỗi khi xóa ước lượng của dự án đã bị đóng hoặc vô hiệu hóa")
    void testDeleteDemand_InactiveProject_ThrowsException() {
        Project inactiveProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-INACTIVE",
                "Dự án đã đóng",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 11, 29),
                BigDecimal.valueOf(100),
                null,
                ProjectStatus.INACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(inactiveProject));

        assertThatThrownBy(() -> service.deleteDemand(PROJECT_ID, ROLE_ID))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("hoạt động");
    }

    @Test
    @DisplayName("Lấy danh sách vai trò chuyên môn dự án thành công")
    void testGetProjectRoles_Success() {
        ProjectRole role1 = new ProjectRole(new ProjectRoleId(1L), "DEV", "Developer", "Lập trình viên");
        ProjectRole role2 = new ProjectRole(new ProjectRoleId(2L), "TEST", "Tester", "Kiểm thử viên");

        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(role1, role2));

        List<com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult> results = service.getProjectRoles();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).code()).isEqualTo("DEV");
        assertThat(results.get(1).code()).isEqualTo("TEST");
        verify(authorizationService).requireAny(
                PermissionCode.PROJECT_READ,
                PermissionCode.PROJECT_RESOURCE_DEMAND_READ,
                PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE);
    }

    @Test
    @DisplayName("Lấy danh sách vai trò chuyên môn dự án bao gồm inactive thành công")
    void testGetProjectRoles_IncludeInactive_Success() {
        ProjectRole role1 = new ProjectRole(new ProjectRoleId(1L), "DEV", "Developer", "Lập trình viên");
        ProjectRole role2 = new ProjectRole(new ProjectRoleId(2L), "TEST", "Tester", "Kiểm thử viên");

        when(loadProjectRolePort.findAll()).thenReturn(List.of(role1, role2));

        List<com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult> results = service.getProjectRoles(true);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).code()).isEqualTo("DEV");
        assertThat(results.get(1).code()).isEqualTo("TEST");
    }

    // ==================== HELPER FACTORIES ====================

    private Project createActiveProject(Long id, LocalDate start, LocalDate end, BigDecimal estHours) {
        return new Project(
                new ProjectId(id),
                "PRJ-IT-261001-ABCDEF",
                "Dự án Thử nghiệm",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                start,
                end,
                estHours,
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    private ProjectResourceDemand createDemand(Long id, int year, int weekNumber, BigDecimal hours) {
        return new ProjectResourceDemand(
                id,
                new ProjectId(PROJECT_ID),
                new ProjectRoleId(ROLE_ID),
                YearWeek.of(year, weekNumber),
                hours,
                LocalDateTime.now(),
                null,
                0L);
    }

    private User createAdminUser() {
        Role adminRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị viên");
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                "admin@hrm.com",
                null,
                1,
                0L);
    }

    private User createPmUser(Long userId, Long employeeId) {
        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        return new User(
                new UserId(userId),
                "pm_user",
                "hash",
                pmRole,
                UserStatus.ACTIVE,
                new EmployeeId(employeeId),
                DataScope.SELF,
                null,
                "pm@hrm.com",
                null,
                1,
                0L);
    }

    private Employee createEmployee(Long id) {
        return new Employee(
                new EmployeeId(id),
                new UserId(CURRENT_USER_ID),
                10L,
                "EMP-001",
                "Nguyen Van PM",
                false,
                40,
                EmployeeStatus.ACTIVE);
    }
}