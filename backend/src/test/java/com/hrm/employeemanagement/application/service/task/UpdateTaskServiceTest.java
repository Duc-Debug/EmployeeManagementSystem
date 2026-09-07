package com.hrm.employeemanagement.application.service.task;

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

import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
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
class UpdateTaskServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long PROJECT_ID = 100L;
    private static final Long TASK_ID = 1L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadProjectPort loadProjectPort;

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

    private UpdateTaskService service;

    @BeforeEach
    void setUp() {
        service = new UpdateTaskService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createCompanyUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_02, "PM"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private Project createActiveProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án thử nghiệm",
                10L,
                new EmployeeId(10L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
    }

    private Task createSampleTask(Long taskId, Long parentId, String name) {
        return new Task(
                new TaskId(taskId),
                new ProjectId(PROJECT_ID),
                parentId != null ? new TaskId(parentId) : null,
                "PRJ-01-T001",
                name,
                "Mô tả cũ",
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                TaskStatus.TODO,
                0,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    @Test
    @DisplayName("Cập nhật thông tin task thành công")
    void testUpdateTask_Success() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task existingTask = createSampleTask(TASK_ID, null, "Tên cũ");
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(existingTask));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskCommand command = new UpdateTaskCommand(
                PROJECT_ID,
                TASK_ID,
                null,
                "Tên mới cập nhật",
                "Mô tả mới",
                null,
                new BigDecimal("25.00"),
                5);

        TaskResult result = service.updateTask(command);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Tên mới cập nhật");
        assertThat(result.description()).isEqualTo("Mô tả mới");
        assertThat(result.estimatedHours()).isEqualTo(new BigDecimal("25.00"));
        assertThat(result.sortOrder()).isEqualTo(5);

        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Chặn chu trình lặp khi đổi parentId sang chính con cháu của task")
    void testUpdateTask_CyclicHierarchy_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        // Task 1 (gốc)
        Task task1 = createSampleTask(1L, null, "Hạng mục 1");
        // Task 2 (con của 1)
        Task task2 = createSampleTask(2L, 1L, "Công việc 2");

        when(loadTaskPort.findById(new TaskId(1L))).thenReturn(Optional.of(task1));
        when(loadTaskPort.findById(new TaskId(2L))).thenReturn(Optional.of(task2));

        // Cố tình chuyển Task 1 làm con của Task 2 (vòng lặp 1 -> 2 -> 1)
        UpdateTaskCommand command = new UpdateTaskCommand(
                PROJECT_ID,
                1L,
                2L,
                "Hạng mục 1",
                null,
                null,
                BigDecimal.ZERO,
                0);

        assertThatThrownBy(() -> service.updateTask(command))
                .isInstanceOf(CyclicTaskHierarchyException.class)
                .hasMessageContaining("Phát hiện chu trình phân cấp");
    }

    @Test
    @DisplayName("Chặn sửa task khi dự án đã đóng (CLOSED)")
    void testUpdateTask_ProjectClosed_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));

        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án đóng",
                10L,
                new EmployeeId(10L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        UpdateTaskCommand command = new UpdateTaskCommand(
                PROJECT_ID,
                TASK_ID,
                null,
                "Tên mới",
                null,
                null,
                BigDecimal.TEN,
                0);

        assertThatThrownBy(() -> service.updateTask(command))
                .isInstanceOf(ProjectClosedException.class);
    }
}
