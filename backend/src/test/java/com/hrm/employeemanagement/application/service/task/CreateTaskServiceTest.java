package com.hrm.employeemanagement.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.hrm.employeemanagement.application.dto.task.CreateTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
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
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
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
class CreateTaskServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long PROJECT_ID = 100L;
    private static final Long ASSIGNEE_ID = 20L;
    private static final Long MANAGER_ID = 10L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private SaveProjectPort saveProjectPort;

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

    private CreateTaskService service;

    @BeforeEach
    void setUp() {
        service = new CreateTaskService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                saveProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createCompanyUser(Long userId) {
        return new User(
                new UserId(userId),
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
                new EmployeeId(MANAGER_ID),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                new BigDecimal("100.00"),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
    }

    @Test
    @DisplayName("TC-01: Tạo task thành công và tự sinh mã tăng dần")
    void testCreateTask_Success_AutoGenerateCode() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        Project project = createActiveProject();
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            return new Task(
                    new TaskId(1L),
                    taskToSave.getProjectId(),
                    taskToSave.getParentId(),
                    taskToSave.getTaskCode(),
                    taskToSave.getName(),
                    taskToSave.getDescription(),
                    taskToSave.getTaskType(),
                    taskToSave.getAssigneeId(),
                    taskToSave.getEstimatedHours(),
                    BigDecimal.ZERO,
                    TaskStatus.TODO,
                    taskToSave.getSortOrder(),
                    taskToSave.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                null,
                "Hạng mục Backend",
                "Mô tả",
                TaskType.CATEGORY,
                null,
                BigDecimal.ZERO,
                1);

        TaskResult result = service.createTask(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.taskCode()).isEqualTo("PRJ-01-T001");
        assertThat(result.name()).isEqualTo("Hạng mục Backend");
        assertThat(result.taskType()).isEqualTo(TaskType.CATEGORY);
        assertThat(project.getTaskSeqCounter()).isEqualTo(1);

        verify(saveProjectPort).save(project);
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Chặn tạo task khi dự án đã đóng (CLOSED)")
    void testCreateTask_ProjectClosed_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án đóng",
                10L,
                new EmployeeId(MANAGER_ID),
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
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                null,
                "Task mới",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                0);

        assertThatThrownBy(() -> service.createTask(command))
                .isInstanceOf(ProjectClosedException.class);
    }

    @Test
    @DisplayName("TC-03: Chặn khi không có quyền phạm vi (DataScope SELF không phải PM)")
    void testCreateTask_PermissionDenied_OutsideScope() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        User selfUser = new User(
                new UserId(CURRENT_USER_ID),
                "pm2",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "PM"),
                UserStatus.ACTIVE,
                null,
                DataScope.SELF,
                null,
                1L);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(selfUser));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.empty());

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                null,
                "Task mới",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                0);

        assertThatThrownBy(() -> service.createTask(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-04: Chặn khi người được phân công không thuộc dự án")
    void testCreateTask_AssigneeNotInProject_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        Project project = createActiveProject();
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Employee assignee = new Employee(
                new EmployeeId(ASSIGNEE_ID),
                new UserId(100L),
                10L,
                "EMP01",
                "Nguyễn Văn A",
                false,
                40,
                EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(ASSIGNEE_ID))).thenReturn(Optional.of(assignee));
        when(loadProjectPort.existsMember(PROJECT_ID, ASSIGNEE_ID)).thenReturn(false);

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                null,
                "Task lập trình",
                null,
                TaskType.TASK,
                ASSIGNEE_ID,
                BigDecimal.TEN,
                0);

        assertThatThrownBy(() -> service.createTask(command))
                .isInstanceOf(AssigneeNotInProjectException.class);
    }

    @Test
    @DisplayName("Chặn khi gán người thực hiện cho CATEGORY")
    void testCreateTask_AssigneeOnCategory_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                null,
                "Hạng mục gom",
                null,
                TaskType.CATEGORY,
                ASSIGNEE_ID,
                BigDecimal.ZERO,
                0);

        assertThatThrownBy(() -> service.createTask(command))
                .isInstanceOf(InvalidTaskDataException.class)
                .hasMessageContaining("Hạng mục gom nhóm không được gán người thực hiện trực tiếp");
    }

    @Test
    @DisplayName("Ném lỗi khi công việc cha không tồn tại")
    void testCreateTask_ParentNotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadTaskPort.findById(new TaskId(999L))).thenReturn(Optional.empty());

        CreateTaskCommand command = new CreateTaskCommand(
                PROJECT_ID,
                999L,
                "Task con",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                0);

        assertThatThrownBy(() -> service.createTask(command))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
