package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTaskProgressService Tests (NCL-04-CN-002)")
class UpdateTaskProgressServiceTest {

    private static final Long USER_ID_A = 10L;
    private static final Long EMPLOYEE_ID_A = 100L;
    private static final Long EMPLOYEE_ID_B = 200L;
    private static final Long TASK_ID = 500L;
    private static final Long PROJECT_ID = 900L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    private UpdateTaskProgressService service;

    private User currentUserA;
    private Employee employeeA;
    private Project activeProject;
    private Task taskInProgress;

    @BeforeEach
    void setUp() {
        service = new UpdateTaskProgressService(
                loadTaskPort,
                saveTaskPort,
                loadTaskAssignmentPort,
                loadProjectPort,
                loadEmployeePort,
                authenticatedUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort
        );

        currentUserA = new User(
                new UserId(USER_ID_A),
                "employee_a",
                "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE,
                new EmployeeId(EMPLOYEE_ID_A),
                DataScope.SELF,
                null,
                1L
        );

        employeeA = new Employee(
                new EmployeeId(EMPLOYEE_ID_A),
                new UserId(USER_ID_A),
                1L,
                "EMP001",
                "Nguyen Van A",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        activeProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án Alpha",
                1L,
                new EmployeeId(999L),
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(30),
                BigDecimal.valueOf(100),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );

        taskInProgress = new Task(
                new TaskId(TASK_ID),
                new ProjectId(PROJECT_ID),
                null,
                "TSK-01",
                "Thiết kế database",
                "Mô tả chi tiết",
                TaskType.TASK,
                new EmployeeId(EMPLOYEE_ID_A),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(25),
                TaskStatus.IN_PROGRESS,
                1,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5),
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5),
                null,
                0,
                new UserId(USER_ID_A),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-01: Chuyển trạng thái từ IN_PROGRESS sang DONE thành công")
    void shouldUpdateProgressFromInProgressToDoneSuccessfully() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.DONE);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(1L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));

        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskProgressResult result = service.updateProgress(command);

        assertNotNull(result);
        assertEquals(TASK_ID, result.taskId());
        assertEquals(PROJECT_ID, result.projectId());
        assertEquals(TaskStatus.IN_PROGRESS, result.previousStatus());
        assertEquals(TaskStatus.DONE, result.currentStatus());

        verify(saveTaskPort).save(taskInProgress);
        // TC-03: Kiểm tra lưu nhật ký kiểm toán
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-01: Chuyển trạng thái từ TODO sang IN_PROGRESS thành công")
    void shouldUpdateProgressFromTodoToInProgressSuccessfully() {
        taskInProgress.updateStatus(TaskStatus.TODO);
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.IN_PROGRESS);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(1L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskProgressResult result = service.updateProgress(command);

        assertEquals(TaskStatus.TODO, result.previousStatus());
        assertEquals(TaskStatus.IN_PROGRESS, result.currentStatus());
        verify(saveTaskPort).save(taskInProgress);
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-01: Cập nhật trùng trạng thái cũ -> Trả về kết quả ngay (No-op, không lưu DB, không ghi log)")
    void shouldReturnImmediatelyWhenStatusIsIdentical() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.IN_PROGRESS);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(1L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));

        TaskProgressResult result = service.updateProgress(command);

        assertEquals(TaskStatus.IN_PROGRESS, result.previousStatus());
        assertEquals(TaskStatus.IN_PROGRESS, result.currentStatus());
        verify(saveTaskPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-02: Người dùng không phải người được giao việc -> Từ chối và ghi denied audit log")
    void shouldRejectWhenUserIsNotAssignedToTask() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.DONE);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.of(employeeA));

        // Công việc được giao cho EMPLOYEE_ID_B (khác với EMPLOYEE_ID_A)
        taskInProgress.assignTo(new EmployeeId(EMPLOYEE_ID_B));
        TaskAssignment assignmentForB = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_B), new UserId(1L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignmentForB));

        assertThrows(TaskNotAssignedToUserException.class, () -> service.updateProgress(command));

        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
        verify(saveTaskPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-02: Người dùng không có hồ sơ nhân sự -> Từ chối (TaskNotAssignedToUserException)")
    void shouldRejectWhenUserHasNoEmployeeProfile() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.DONE);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.empty());
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(Collections.emptyList());

        assertThrows(TaskNotAssignedToUserException.class, () -> service.updateProgress(command));
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
        verify(saveTaskPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-03: Ghi nhận đúng thông tin trong AuditLog khi đổi trạng thái")
    void shouldRecordAccurateAuditLogDetails() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.IN_REVIEW);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(new UserId(USER_ID_A))).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(1L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProgress(command);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());

        AuditLog capturedLog = auditCaptor.getValue();
        assertEquals(USER_ID_A, capturedLog.getUserId());
        assertEquals("UPDATE_TASK_PROGRESS", capturedLog.getAction());
        assertEquals("tasks", capturedLog.getTableName());
        assertEquals(TASK_ID, capturedLog.getRecordId());
        assertEquals("IN_PROGRESS", capturedLog.getOldValue());
        assertEquals("IN_REVIEW", capturedLog.getNewValue());
        assertNotNull(capturedLog.getCreatedAt());
    }

    @Test
    @DisplayName("Quy tắc QTN-08: Chặn khi dự án ở trạng thái đóng (CLOSED)")
    void shouldRejectWhenProjectIsClosed() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.DONE);

        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án Alpha",
                1L,
                new EmployeeId(999L),
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(30),
                BigDecimal.valueOf(100),
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        assertThrows(ProjectClosedException.class, () -> service.updateProgress(command));
        verify(saveTaskPort, never()).save(any());
    }

    @Test
    @DisplayName("Ngoại lệ: Chặn khi trạng thái mới là CANCELLED (nhân viên chuyên môn không được tự hủy việc)")
    void shouldRejectWhenStatusIsCancelled() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(TASK_ID, TaskStatus.CANCELLED);

        InvalidTaskDataException ex = assertThrows(InvalidTaskDataException.class,
                () -> service.updateProgress(command));
        assertEquals("Nhân viên chuyên môn không được phép hủy công việc (CANCELLED)", ex.getMessage());
        verify(saveTaskPort, never()).save(any());
    }

    @Test
    @DisplayName("Ngoại lệ: Ném TaskNotFoundException khi công việc không tồn tại")
    void shouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        UpdateTaskProgressCommand command = new UpdateTaskProgressCommand(99999L, TaskStatus.DONE);

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(99999L))).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.updateProgress(command));
        verify(saveTaskPort, never()).save(any());
    }

    @Test
    @DisplayName("Ngoại lệ: Ném InvalidTaskDataException khi command hoặc tham số null")
    void shouldThrowInvalidTaskDataExceptionWhenArgumentsAreNull() {
        assertThrows(InvalidTaskDataException.class, () -> service.updateProgress(null));
        assertThrows(InvalidTaskDataException.class, () -> service.updateProgress(new UpdateTaskProgressCommand(null, TaskStatus.DONE)));
        assertThrows(InvalidTaskDataException.class, () -> service.updateProgress(new UpdateTaskProgressCommand(TASK_ID, null)));
    }
}
