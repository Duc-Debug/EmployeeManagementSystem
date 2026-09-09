package com.hrm.employeemanagement.application.service.project;

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

import com.hrm.employeemanagement.application.dto.project.RemoveProjectMemberCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException;
import com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
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
class RemoveProjectMemberServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long PM_EMPLOYEE_ID = 50L;
    private static final Long MEMBER_EMPLOYEE_ID = 60L;
    private static final Long ORG_UNIT_ID = 100L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    private SaveProjectMemberPort saveProjectMemberPort;
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

    private RemoveProjectMemberService service;

    @BeforeEach
    void setUp() {
        service = new RemoveProjectMemberService(
                loadProjectPort,
                loadProjectMemberPort,
                saveProjectMemberPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );
    }

    private User createAdminUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_06, "Admin"),
                UserStatus.ACTIVE,
                new EmployeeId(PM_EMPLOYEE_ID),
                DataScope.COMPANY,
                null,
                1L
        );
    }

    private Project createActiveProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Project Alpha",
                ORG_UNIT_ID,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO,
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("Ném ngoại lệ khi command null hoặc thiếu thông tin")
    void shouldThrowWhenCommandIsInvalid() {
        assertThatThrownBy(() -> service.removeProjectMember(null))
                .isInstanceOf(InvalidProjectDataException.class);

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(null, 60L)))
                .isInstanceOf(InvalidProjectDataException.class);

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(1L, null)))
                .isInstanceOf(InvalidProjectDataException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy dự án")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi dự án đã CLOSED")
    void shouldThrowWhenProjectIsClosed() {
        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Project Alpha",
                ORG_UNIT_ID,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO,
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(ProjectClosedException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi cố gắng xóa PM khỏi danh sách thành viên")
    void shouldThrowWhenRemovingPM() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, PM_EMPLOYEE_ID)))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("Quản lý dự án (PM)");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi thành viên không tồn tại trong dự án")
    void shouldThrowWhenMemberNotFoundInProject() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadProjectMemberPort.existsMember(PROJECT_ID, MEMBER_EMPLOYEE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi thành viên còn task chưa hoàn thành trong dự án")
    void shouldThrowWhenMemberHasActiveTasks() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadProjectMemberPort.existsMember(PROJECT_ID, MEMBER_EMPLOYEE_ID)).thenReturn(true);
        when(loadProjectMemberPort.hasActiveTasks(PROJECT_ID, MEMBER_EMPLOYEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(MemberHasActiveTasksException.class);

        verify(saveProjectMemberPort, never()).removeMember(any(), any());
    }

    @Test
    @DisplayName("Xóa thành viên khỏi dự án thành công")
    void shouldRemoveMemberSuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadProjectMemberPort.existsMember(PROJECT_ID, MEMBER_EMPLOYEE_ID)).thenReturn(true);
        when(loadProjectMemberPort.hasActiveTasks(PROJECT_ID, MEMBER_EMPLOYEE_ID)).thenReturn(false);

        service.removeProjectMember(new RemoveProjectMemberCommand(PROJECT_ID, MEMBER_EMPLOYEE_ID));

        verify(saveProjectMemberPort).removeMember(PROJECT_ID, MEMBER_EMPLOYEE_ID);
        verify(saveAuditLogPort).save(any());
    }
}
