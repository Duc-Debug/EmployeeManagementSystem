package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.UpdateProjectCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
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
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class UpdateProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private SaveProjectPort saveProjectPort;
    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private UpdateProjectService updateProjectService;

    @BeforeEach
    void setUp() {
        updateProjectService = new UpdateProjectService(
                loadProjectPort,
                saveProjectPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    @Test
    @DisplayName("Cập nhật dự án thành công khi hợp lệ")
    void shouldUpdateProjectSuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        User currentUser = createTestUser(CURRENT_USER_ID, DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentUser));

        Project existingProject = createTestProject(PROJECT_ID, ProjectStatus.ACTIVE);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(existingProject));

        Employee manager = createTestEmployee(MANAGER_ID, ORG_UNIT_ID, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(MANAGER_ID))).thenReturn(Optional.of(manager));

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProjectCommand command = new UpdateProjectCommand(
                PROJECT_ID,
                "Tên dự án mới",
                MANAGER_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("500.0"),
                "Mô tả mới");

        ProjectResult result = updateProjectService.updateProject(command);

        assertThat(result).isNotNull();
        assertThat(result.getProjectName()).isEqualTo("Tên dự án mới");
        assertThat(result.getEstimatedHours()).isEqualTo(new BigDecimal("500.0"));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Ném lỗi khi không tìm thấy dự án")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        User currentUser = createTestUser(CURRENT_USER_ID, DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentUser));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        UpdateProjectCommand command = new UpdateProjectCommand(
                PROJECT_ID, "Tên mới", null, null, null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> updateProjectService.updateProject(command))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Ném lỗi khi dự án không ở trạng thái hoạt động (ACTIVE)")
    void shouldThrowWhenProjectNotActive() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        User currentUser = createTestUser(CURRENT_USER_ID, DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentUser));

        Project existingProject = createTestProject(PROJECT_ID, ProjectStatus.INACTIVE);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(existingProject));

        UpdateProjectCommand command = new UpdateProjectCommand(
                PROJECT_ID, "Tên mới", null, null, null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> updateProjectService.updateProject(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("trạng thái hoạt động");
    }

    @Test
    @DisplayName("Ném lỗi khi người dùng ngoài phạm vi dữ liệu (DataScope)")
    void shouldThrowWhenUserOutsideDataScope() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        User currentUser = createTestUser(CURRENT_USER_ID, DataScope.ORGANIZATION_BRANCH);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentUser));

        Project existingProject = createTestProject(PROJECT_ID, ProjectStatus.ACTIVE);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(existingProject));
        when(loadProjectPort.existsInOrgUnitBranch(PROJECT_ID, currentUser.getScopeOrgUnitId())).thenReturn(false);

        UpdateProjectCommand command = new UpdateProjectCommand(
                PROJECT_ID, "Tên mới", null, null, null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> updateProjectService.updateProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    private User createTestUser(Long id, DataScope dataScope) {
        Role role = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        Long scopeOrgUnitId = (dataScope == DataScope.ORGANIZATION_BRANCH) ? 999L : null;
        return new User(
                new UserId(id),
                "pm_user",
                "hash",
                role,
                UserStatus.ACTIVE,
                new EmployeeId(MANAGER_ID),
                dataScope,
                scopeOrgUnitId,
                0L);
    }

    private Project createTestProject(Long id, ProjectStatus status) {
        return new Project(
                new ProjectId(id),
                "PRJ-TEST-001",
                "Dự án ban đầu",
                ORG_UNIT_ID,
                new EmployeeId(MANAGER_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("100.0"),
                "Mô tả ban đầu",
                status,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    private Employee createTestEmployee(Long id, Long orgUnitId, EmployeeStatus status) {
        return new Employee(
                new EmployeeId(id),
                new UserId(100L + id),
                orgUnitId,
                "EMP-" + id,
                "Nguyễn Văn PM",
                false,
                40,
                status);
    }
}
