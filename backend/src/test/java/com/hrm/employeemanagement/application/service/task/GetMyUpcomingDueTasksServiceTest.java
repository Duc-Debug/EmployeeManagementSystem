package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class GetMyUpcomingDueTasksServiceTest {

    private GetAuthenticatedUserPort getAuthenticatedUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadTaskDueReminderPort loadTaskDueReminderPort;
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    private GetMyUpcomingDueTasksService service;

    @BeforeEach
    void setUp() {
        getAuthenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadTaskDueReminderPort = mock(LoadTaskDueReminderPort.class);
        deniedAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);

        service = new GetMyUpcomingDueTasksService(
                getAuthenticatedUserPort,
                loadEmployeePort,
                loadTaskDueReminderPort,
                deniedAuditLogPort
        );
    }

    private User createUserWithRole(Long userId, RoleCode roleCode) {
        Role role = new Role(new com.hrm.employeemanagement.domain.role.RoleId(userId), roleCode, roleCode.getName());
        return new User(
                new UserId(userId),
                "test_user",
                "hash",
                role,
                UserStatus.ACTIVE,
                new EmployeeId(1L)
        );
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-03: Không có quyền - Người dùng không thuộc VT-04 bị từ chối truy cập và ghi nhật ký")
    void tc03_unauthorizedUser_throwsExceptionAndLogsAudit() {
        // Given: User có vai trò Quản lý dự án (VT-02), không phải Nhân viên chuyên môn (VT-04)
        User pmUser = createUserWithRole(99L, RoleCode.VT_02);
        when(getAuthenticatedUserPort.getAuthenticatedUser()).thenReturn(pmUser);

        // When & Then: Ném NotificationAccessDeniedException (được GlobalExceptionHandler map thành HTTP 403)
        NotificationAccessDeniedException ex = assertThrows(
                NotificationAccessDeniedException.class,
                () -> service.execute()
        );
        assertTrue(ex.getMessage().contains("VT-04"));

        // Kiểm tra ghi nhật ký từ chối truy cập vào audit log
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertNotNull(auditLog);
        assertEquals(99L, auditLog.getUserId());
        assertEquals("PERMISSION_DENIED", auditLog.getAction());
        assertEquals("task_due_reminders", auditLog.getTableName());
        assertTrue(auditLog.getNewValue().contains("role=VT-02"));
        assertTrue(auditLog.getNewValue().contains("reason=ROLE_NOT_SPECIALIST"));
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-03: Có quyền - Nhân viên chuyên môn (VT-04) lấy danh sách công việc sắp đến hạn thành công")
    void tc03_authorizedSpecialist_returnsUpcomingTasks() {
        // Given: User thuộc vai trò Nhân viên chuyên môn (VT-04)
        User specialistUser = createUserWithRole(50L, RoleCode.VT_04);
        Employee employee = new Employee(
                new EmployeeId(5L),
                new UserId(50L),
                1L,
                "EMP-005",
                "Chuyên viên DEV",
                "Developer",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(2);
        Task task = new Task(
                new TaskId(201L),
                new ProjectId(10L),
                null,
                "TSK-201",
                "Thiết kế giao diện",
                "Mô tả",
                TaskType.TASK,
                new EmployeeId(5L),
                BigDecimal.valueOf(8),
                BigDecimal.ZERO,
                BigDecimal.valueOf(8),
                TaskStatus.IN_PROGRESS,
                1,
                today,
                dueDate,
                today,
                dueDate,
                null,
                0,
                new UserId(1L),
                today.atStartOfDay(),
                null,
                1L
        );

        when(getAuthenticatedUserPort.getAuthenticatedUser()).thenReturn(specialistUser);
        when(loadEmployeePort.findByUserId(new UserId(50L))).thenReturn(Optional.of(employee));
        when(loadTaskDueReminderPort.findUpcomingTasksByAssignee(eq(new EmployeeId(5L)), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(task));

        // When
        List<UpcomingDueTaskResult> results = service.execute();

        // Then
        assertEquals(1, results.size());
        UpcomingDueTaskResult item = results.get(0);
        assertEquals(201L, item.taskId());
        assertEquals(10L, item.projectId());
        assertEquals("Thiết kế giao diện", item.taskName());
        assertEquals(2, item.daysRemaining());
        assertEquals("/projects/10/tasks/201", item.directUrl());
        assertEquals("IN_PROGRESS", item.status());
    }
}
