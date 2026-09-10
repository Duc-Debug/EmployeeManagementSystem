package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.SetTaskBudgetCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBudgetResult;
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
import com.hrm.employeemanagement.domain.task.TaskBudgetBurnStatus;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class SetTaskBudgetServiceTest {

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

    private SetTaskBudgetService service;

    @BeforeEach
    void setUp() {
        service = new SetTaskBudgetService(
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
                "pm_user",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc"),
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
                "Dự án ERP",
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

    private Task createTaskWithActualHours(BigDecimal actualHours) {
        return new Task(
                new TaskId(TASK_ID),
                new ProjectId(PROJECT_ID),
                null,
                "PRJ-T001",
                "Khảo sát người dùng",
                "Chi tiết khảo sát",
                TaskType.TASK,
                new EmployeeId(5L),
                new BigDecimal("40.00"),
                actualHours,
                BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS,
                1,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                1L);
    }

    @Test
    @DisplayName("PM đặt ngân sách giờ công thành công khi còn an toàn (< 80%)")
    void shouldSetTaskBudgetSuccessfully_SafeStatus() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTaskWithActualHours(new BigDecimal("20.00"));
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        SetTaskBudgetCommand command = new SetTaskBudgetCommand(PROJECT_ID, TASK_ID, new BigDecimal("50.00"));
        TaskBudgetResult result = service.setTaskBudget(command);

        assertThat(result).isNotNull();
        assertThat(result.budgetHours()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.actualHours()).isEqualTo(new BigDecimal("20.00"));
        assertThat(result.burnedPercentage()).isEqualTo(new BigDecimal("40.00"));
        assertThat(result.burnStatus()).isEqualTo(TaskBudgetBurnStatus.SAFE);
        assertThat(result.isOverBudget()).isFalse();
        assertThat(result.remainingHours()).isEqualTo(new BigDecimal("30.00"));

        verify(saveTaskPort).save(any(Task.class));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("PM đặt ngân sách giờ công và phát hiện vượt ngân sách (OVER_BUDGET)")
    void shouldDetectOverBudget_WhenActualHoursExceedBudget() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTaskWithActualHours(new BigDecimal("55.00"));
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        SetTaskBudgetCommand command = new SetTaskBudgetCommand(PROJECT_ID, TASK_ID, new BigDecimal("50.00"));
        TaskBudgetResult result = service.setTaskBudget(command);

        assertThat(result).isNotNull();
        assertThat(result.budgetHours()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.actualHours()).isEqualTo(new BigDecimal("55.00"));
        assertThat(result.burnedPercentage()).isEqualTo(new BigDecimal("110.00"));
        assertThat(result.burnStatus()).isEqualTo(TaskBudgetBurnStatus.OVER_BUDGET);
        assertThat(result.isOverBudget()).isTrue();
        assertThat(result.remainingHours()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.overBudgetHours()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngân sách giờ công âm")
    void shouldThrowInvalidTaskDataException_WhenBudgetNegative() {
        SetTaskBudgetCommand command = new SetTaskBudgetCommand(PROJECT_ID, TASK_ID, new BigDecimal("-10.00"));

        assertThatThrownBy(() -> service.setTaskBudget(command))
                .isInstanceOf(InvalidTaskDataException.class)
                .hasMessageContaining("Ngân sách giờ công không được nhỏ hơn 0");
    }

    @Test
    @DisplayName("Ném ProjectClosedException khi dự án đã đóng (CLOSED)")
    void shouldThrowProjectClosedException_WhenProjectIsClosed() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));

        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án đã đóng",
                10L,
                new EmployeeId(10L),
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);

        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        SetTaskBudgetCommand command = new SetTaskBudgetCommand(PROJECT_ID, TASK_ID, new BigDecimal("40.00"));

        assertThatThrownBy(() -> service.setTaskBudget(command))
                .isInstanceOf(ProjectClosedException.class);
    }

    @Test
    @DisplayName("Ném TaskNotFoundException khi không tìm thấy công việc")
    void shouldThrowTaskNotFoundException_WhenTaskNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.empty());

        SetTaskBudgetCommand command = new SetTaskBudgetCommand(PROJECT_ID, TASK_ID, new BigDecimal("40.00"));

        assertThatThrownBy(() -> service.setTaskBudget(command))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
