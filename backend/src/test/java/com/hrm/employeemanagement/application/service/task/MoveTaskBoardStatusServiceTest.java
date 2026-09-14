package com.hrm.employeemanagement.application.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
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
@DisplayName("MoveTaskBoardStatusService Tests (NCL-04-CN-006)")
class MoveTaskBoardStatusServiceTest {

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

    private MoveTaskBoardStatusService service;

    private User currentUserA;
    private Employee employeeA;
    private Project activeProject;
    private Task taskInProgress;

    @BeforeEach
    void setUp() {
        service = new MoveTaskBoardStatusService(
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
    @DisplayName("NCL-04-CN-006-TC-01: Kéo thả thẻ từ Đang làm sang Chờ duyệt thành công")
    void testMoveStatus_Success_TC01() {
        // Given: Người dùng A có công việc ở trạng thái đang làm (IN_PROGRESS)
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(currentUserA.getId())).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(USER_ID_A), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));

        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Kéo thả sang cột chờ duyệt (IN_REVIEW)
        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.IN_REVIEW);
        TaskBoardCardResult result = service.moveTaskStatus(command);

        // Then: Trạng thái công việc được cập nhật ngay thành IN_REVIEW
        assertNotNull(result);
        assertEquals(TaskStatus.IN_REVIEW, result.status());
        assertEquals(TaskStatus.IN_REVIEW, taskInProgress.getStatus());
        verify(saveTaskPort).save(taskInProgress);
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-02: Thẻ thuộc công việc của người khác -> Chặn và giữ nguyên trạng thái (403 FORBIDDEN)")
    void testMoveStatus_Forbidden_DifferentUser_TC02() {
        // Given: Thẻ thuộc công việc được giao cho nhân viên B (EMPLOYEE_ID_B), người thực hiện là nhân viên A
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(currentUserA.getId())).thenReturn(Optional.of(employeeA));

        TaskAssignment assignmentB = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_B), new UserId(99L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignmentB));

        // When: Kéo thả thẻ đó sang cột khác (IN_REVIEW)
        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.IN_REVIEW);

        // Then: Hệ thống chặn và ném TaskNotAssignedToUserException
        assertThrows(TaskNotAssignedToUserException.class, () -> service.moveTaskStatus(command));

        // Trạng thái task vẫn giữ nguyên trạng thái cũ (IN_PROGRESS)
        assertEquals(TaskStatus.IN_PROGRESS, taskInProgress.getStatus());
        verify(saveTaskPort, never()).save(any());

        // Hệ thống ghi lại audit log từ chối truy cập
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-03: Lưu lịch sử thao tác kéo thả (Audit Log ghi lại người thực hiện, nội dung, thời điểm)")
    void testMoveStatus_AuditLogSaved_TC03() {
        // Given
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(currentUserA.getId())).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(USER_ID_A), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.IN_REVIEW);
        service.moveTaskStatus(command);

        // Then: Ghi lại người thực hiện, nội dung và thời điểm
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());

        AuditLog capturedLog = auditCaptor.getValue();
        assertEquals(USER_ID_A, capturedLog.getUserId());
        assertEquals("MOVE_TASK_BOARD_STATUS", capturedLog.getAction());
        assertEquals("tasks", capturedLog.getTableName());
        assertEquals(TASK_ID, capturedLog.getRecordId());
        assertEquals("IN_PROGRESS", capturedLog.getOldValue());
        assertEquals("IN_REVIEW", capturedLog.getNewValue());
        assertNotNull(capturedLog.getCreatedAt());
    }

    @Test
    @DisplayName("Quản lý dự án (PM) có quyền kéo thả thẻ công việc của thành viên trong dự án")
    void testMoveStatus_PMCanMove_Success() {
        // Project activeProject được quản lý bởi EMPLOYEE_ID_A
        Project managedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án Alpha",
                1L,
                new EmployeeId(EMPLOYEE_ID_A), // PM là A
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

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(managedProject));
        when(loadEmployeePort.findByUserId(currentUserA.getId())).thenReturn(Optional.of(employeeA));

        // Task này được giao cho B, không phải A
        TaskAssignment assignmentB = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_B), new UserId(99L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignmentB));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: PM kéo thẻ của thành viên B sang DONE
        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.DONE);
        TaskBoardCardResult result = service.moveTaskStatus(command);

        // Then: Thành công
        assertEquals(TaskStatus.DONE, result.status());
        verify(saveTaskPort).save(taskInProgress);
    }

    @Test
    @DisplayName("Báo lỗi khi dự án đã bị đóng (ProjectClosedException)")
    void testMoveStatus_ProjectClosed_ThrowsException() {
        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án Alpha",
                1L,
                new EmployeeId(EMPLOYEE_ID_A),
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

        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.DONE);

        assertThrows(ProjectClosedException.class, () -> service.moveTaskStatus(command));
        verify(saveTaskPort, never()).save(any());
    }

    @Test
    @DisplayName("Báo lỗi khi công việc không tồn tại (TaskNotFoundException)")
    void testMoveStatus_TaskNotFound_ThrowsException() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(9999L))).thenReturn(Optional.empty());

        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(9999L, TaskStatus.IN_REVIEW);

        assertThrows(TaskNotFoundException.class, () -> service.moveTaskStatus(command));
    }

    @Test
    @DisplayName("Kéo thả thẻ vào cùng cột trạng thái hiện tại -> Idempotent / No-op (không lưu lại và không spam audit)")
    void testMoveStatus_SameStatus_NoOp() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUserA);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress)); // IN_PROGRESS
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(currentUserA.getId())).thenReturn(Optional.of(employeeA));

        TaskAssignment assignment = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_A), new UserId(USER_ID_A), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignment));

        // When: Kéo thả lại vào chính IN_PROGRESS
        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.IN_PROGRESS);
        TaskBoardCardResult result = service.moveTaskStatus(command);

        assertEquals(TaskStatus.IN_PROGRESS, result.status());
        verify(saveTaskPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Báo lỗi khi tham số đầu vào không hợp lệ (InvalidTaskDataException)")
    void testMoveStatus_InvalidInput_ThrowsException() {
        assertThrows(InvalidTaskDataException.class, () -> service.moveTaskStatus(null));
        assertThrows(InvalidTaskDataException.class, () -> service.moveTaskStatus(new MoveTaskBoardStatusCommand(null, TaskStatus.IN_REVIEW)));
        assertThrows(InvalidTaskDataException.class, () -> service.moveTaskStatus(new MoveTaskBoardStatusCommand(TASK_ID, null)));
    }

    @Test
    @DisplayName("Người dùng có COMPANY scope nhưng không phải PM và không được giao việc -> Bị chặn (TaskNotAssignedToUserException)")
    void testMoveStatus_Forbidden_CompanyScopeUser_NotPM_NotAssignee() {
        User companyAdmin = new User(
                new UserId(99L),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Quản trị hệ thống"),
                UserStatus.ACTIVE,
                new EmployeeId(9999L),
                DataScope.COMPANY,
                null,
                1L
        );
        Employee adminEmp = new Employee(
                new EmployeeId(9999L),
                new UserId(99L),
                1L,
                "ADMIN01",
                "Admin User",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(companyAdmin);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(taskInProgress));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadEmployeePort.findByUserId(companyAdmin.getId())).thenReturn(Optional.of(adminEmp));

        // Task giao cho B, PM là người khác (999L)
        TaskAssignment assignmentB = TaskAssignment.create(new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID_B), new UserId(88L), true);
        when(loadTaskAssignmentPort.findByTaskId(new TaskId(TASK_ID))).thenReturn(List.of(assignmentB));

        MoveTaskBoardStatusCommand command = new MoveTaskBoardStatusCommand(TASK_ID, TaskStatus.DONE);

        assertThrows(TaskNotAssignedToUserException.class, () -> service.moveTaskStatus(command));
        verify(saveTaskPort, never()).save(any());
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }
}
