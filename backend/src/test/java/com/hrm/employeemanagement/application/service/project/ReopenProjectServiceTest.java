package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.hrm.employeemanagement.application.dto.project.ReopenProjectCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotClosedException;
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
class ReopenProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private SaveProjectPort saveProjectPort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private ReopenProjectService reopenProjectService;

    @BeforeEach
    void setUp() {
        reopenProjectService = new ReopenProjectService(
                loadProjectPort,
                saveProjectPort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createExecutiveUser() {
        Role execRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc");
        return new User(
                new UserId(CURRENT_USER_ID),
                "director.user",
                "hashed_pwd",
                execRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private User createAdminUser() {
        Role adminRole = new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên");
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin.user",
                "hashed_pwd",
                adminRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private User createPmUser() {
        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        return new User(
                new UserId(CURRENT_USER_ID),
                "pm.user",
                "hashed_pwd",
                pmRole,
                UserStatus.ACTIVE,
                new EmployeeId(MANAGER_ID),
                DataScope.SELF,
                null,
                1L);
    }

    private Project createClosedProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-2026-001",
                "Dự án ERP",
                ORG_UNIT_ID,
                new EmployeeId(MANAGER_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(1L),
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1),
                0L,
                0,
                "Đóng bàn giao",
                LocalDateTime.now().minusDays(1),
                new UserId(1L),
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) mở lại dự án thành công với lý do hợp lệ >= 10 ký tự")
    void shouldReopenProjectSuccessfullyWhenExecutive() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createClosedProject()));
        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "Mở lại dự án theo phụ lục hợp đồng số 02");
        ProjectResult result = reopenProjectService.reopenProject(command);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(result.getReopenReason()).isEqualTo("Mở lại dự án theo phụ lục hợp đồng số 02");
        assertThat(result.getReopenedBy()).isEqualTo(CURRENT_USER_ID);
        assertThat(result.getReopenedAt()).isNotNull();
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Admin (VT-06) không có quyền mở lại dự án -> Bị từ chối 403")
    void shouldDenyAdminFromReopeningProject() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "Admin cố tình mở lại dự án");

        assertThatThrownBy(() -> reopenProjectService.reopenProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any());
        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("PM (VT-02) không có quyền mở lại dự án -> Bị từ chối 403")
    void shouldDenyPmFromReopeningProject() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "PM cố tình mở lại dự án để khai thêm giờ");

        assertThatThrownBy(() -> reopenProjectService.reopenProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any());
        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi lý do mở lại dưới 10 ký tự hoặc null")
    void shouldThrowWhenReopenReasonIsTooShort() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "Quá ngắn");

        assertThatThrownBy(() -> reopenProjectService.reopenProject(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("ít nhất 10 ký tự");

        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi mở lại dự án đang ở trạng thái ACTIVE (chưa đóng)")
    void shouldThrowWhenProjectIsNotClosed() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));

        Project activeProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-2026-001",
                "Dự án ERP",
                ORG_UNIT_ID,
                new EmployeeId(MANAGER_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L,
                0);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "Lý do mở lại dự án hợp lệ trên 10 ký tự");

        assertThatThrownBy(() -> reopenProjectService.reopenProject(command))
                .isInstanceOf(ProjectNotClosedException.class)
                .hasMessageContaining("Chỉ có thể mở lại dự án đang ở trạng thái đóng");

        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi dự án không tồn tại")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_REOPEN)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        ReopenProjectCommand command = new ReopenProjectCommand(PROJECT_ID, "Lý do mở lại dự án hợp lệ trên 10 ký tự");

        assertThatThrownBy(() -> reopenProjectService.reopenProject(command))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
