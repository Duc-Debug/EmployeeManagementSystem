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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.CloseProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectHasUnfinishedTasksException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class CloseProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private SaveProjectPort saveProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
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

    private CloseProjectService closeProjectService;

    @BeforeEach
    void setUp() {
        closeProjectService = new CloseProjectService(
                loadProjectPort,
                saveProjectPort,
                loadTaskPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
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

    private Project createActiveProject() {
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
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L,
                0);
    }

    @Test
    @DisplayName("PM trực tiếp đóng dự án thành công khi mọi task đã hoàn thành")
    void shouldCloseProjectSuccessfullyWhenPmDirectManager() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Employee pmEmployee = new Employee(
                new EmployeeId(MANAGER_ID),
                new UserId(CURRENT_USER_ID),
                ORG_UNIT_ID,
                "EMP001",
                "PM",
                false,
                40,
                EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(pmEmployee));

        Task completedTask = new Task(
                new TaskId(10L),
                new ProjectId(PROJECT_ID),
                null,
                "TSK-001",
                "Task đã xong",
                null,
                TaskType.TASK,
                new EmployeeId(MANAGER_ID),
                BigDecimal.TEN,
                BigDecimal.TEN,
                TaskStatus.DONE,
                1,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(completedTask));
        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Nghiệm thu hoàn tất");
        ProjectResult result = closeProjectService.closeProject(command);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(result.getClosureReason()).isEqualTo("Nghiệm thu hoàn tất");
        assertThat(result.getClosedBy()).isEqualTo(CURRENT_USER_ID);
        assertThat(result.getClosedAt()).isNotNull();
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) đóng hộ thành công khi có lý do >= 10 ký tự")
    void shouldCloseProjectSuccessfullyWhenExecutiveWithValidReason() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(Collections.emptyList());
        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "PM nghỉ việc, ban giám đốc phê duyệt đóng dự án");
        ProjectResult result = closeProjectService.closeProject(command);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(result.getClosureReason()).contains("PM nghỉ việc");
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) bị từ chối khi lý do đóng dưới 10 ký tự hoặc null")
    void shouldThrowWhenExecutiveProvidesReasonLessThan10Chars() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Quá ngắn");

        assertThatThrownBy(() -> closeProjectService.closeProject(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("tối thiểu 10 ký tự");

        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Admin (VT-06) không có quyền đóng dự án -> ném PermissionDeniedException")
    void shouldDenyAdminFromClosingProject() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Admin đóng dự án này");

        assertThatThrownBy(() -> closeProjectService.closeProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any());
        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("PM khác (không quản lý dự án) bị từ chối truy cập")
    void shouldDenyWhenPmIsNotDirectManager() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        // PM này có ID 999 khác với MANAGER_ID 50L
        Employee otherEmployee = new Employee(
                new EmployeeId(999L),
                new UserId(CURRENT_USER_ID),
                ORG_UNIT_ID,
                "EMP999",
                "Other PM",
                false,
                40,
                EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(otherEmployee));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Thử đóng dự án người khác");

        assertThatThrownBy(() -> closeProjectService.closeProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any());
        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Chặn đóng dự án khi còn task WBS ở trạng thái TODO hoặc IN_PROGRESS")
    void shouldThrowWhenProjectHasUnfinishedTasks() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task unfinishedTask = new Task(
                new TaskId(11L),
                new ProjectId(PROJECT_ID),
                null,
                "TSK-002",
                "Công việc còn dang dở",
                null,
                TaskType.TASK,
                new EmployeeId(MANAGER_ID),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS,
                1,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(unfinishedTask));

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Đóng dự án với lý do hợp lệ dài hơn 10 ký tự");

        assertThatThrownBy(() -> closeProjectService.closeProject(command))
                .isInstanceOf(ProjectHasUnfinishedTasksException.class)
                .hasMessageContaining("TSK-002");

        verify(saveProjectPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném ProjectNotFoundException khi dự án không tồn tại")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_CLOSE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createExecutiveUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        CloseProjectCommand command = new CloseProjectCommand(PROJECT_ID, "Lý do hợp lệ dài hơn 10 ký tự");

        assertThatThrownBy(() -> closeProjectService.closeProject(command))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
