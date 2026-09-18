package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.task.CheckTaskDueReminderSentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.UserId;

class TaskDueReminderApplicationServiceTest {

    private LoadTaskDueReminderPort loadTaskDueReminderPort;
    private CheckTaskDueReminderSentPort checkTaskDueReminderSentPort;
    private LoadEmployeePort loadEmployeePort;
    private SaveNotificationPort saveNotificationPort;
    private CreateNotificationEventUseCase createNotificationEventUseCase;
    private SaveAuditLogPort saveAuditLogPort;

    private TaskDueReminderApplicationService service;

    private final LocalDate scanDate = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        loadTaskDueReminderPort = mock(LoadTaskDueReminderPort.class);
        checkTaskDueReminderSentPort = mock(CheckTaskDueReminderSentPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        saveNotificationPort = mock(SaveNotificationPort.class);
        createNotificationEventUseCase = mock(CreateNotificationEventUseCase.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new TaskDueReminderApplicationService(
                loadTaskDueReminderPort,
                checkTaskDueReminderSentPort,
                loadEmployeePort,
                saveNotificationPort,
                createNotificationEventUseCase,
                saveAuditLogPort
        );
    }

    private Task createTask(Long id, Long projectId, String name, Long assigneeId, LocalDate dueDate, TaskStatus status) {
        return new Task(
                new TaskId(id),
                new ProjectId(projectId),
                null,
                "TSK-00" + id,
                name,
                "Task Description",
                TaskType.TASK,
                new EmployeeId(assigneeId),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                status,
                1,
                dueDate.minusDays(5),
                dueDate,
                dueDate.minusDays(5),
                dueDate,
                null,
                0,
                new UserId(1L),
                scanDate.atStartOfDay(),
                null,
                1L
        );
    }

    private Employee createEmployee(Long id, Long userId, String fullName) {
        return new Employee(
                new EmployeeId(id),
                new UserId(userId),
                1L,
                "EMP-00" + id,
                fullName,
                "Lập trình viên",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-01: Gửi thông báo khi công việc còn 2 ngày là tới hạn")
    void tc01_sendReminder_whenTaskIsDueInTwoDays() {
        LocalDate taskDueDate = scanDate.plusDays(2);
        Task task = createTask(101L, 10L, "Xây dựng tính năng đăng nhập", 5L, taskDueDate, TaskStatus.IN_PROGRESS);
        Employee employee = createEmployee(5L, 50L, "Nguyễn Văn Chuyên Môn");

        when(loadTaskDueReminderPort.findTasksDueBetween(scanDate, scanDate.plusDays(3))).thenReturn(List.of(task));
        when(loadEmployeePort.findById(new EmployeeId(5L))).thenReturn(Optional.of(employee));
        when(checkTaskDueReminderSentPort.hasReminderBeenSent(new UserId(50L), 101L, taskDueDate)).thenReturn(false);

        TaskDueReminderScanResult result = service.execute(scanDate);

        assertEquals(1, result.totalScanned());
        assertEquals(1, result.sentCount());
        assertEquals(0, result.skippedDuplicateCount());
        assertEquals(0, result.skippedCompletedCount());
        assertEquals(List.of(101L), result.notifiedTaskIds());

        // Kiểm tra thông báo lưu vào SaveNotificationPort
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(notifCaptor.capture());
        Notification savedNotif = notifCaptor.getValue();
        assertEquals(50L, savedNotif.getRecipientId().value());
        assertEquals(NotificationType.TASK_DUE_REMINDER, savedNotif.getType());
        assertEquals("TASK", savedNotif.getTargetType());
        assertEquals(101L, savedNotif.getTargetId());
        assertTrue(savedNotif.getTitle().contains("Xây dựng tính năng đăng nhập"));
        assertTrue(savedNotif.getContent().contains("/projects/10/tasks/101"));

        // Kiểm tra thông báo lưu vào CreateNotificationEventUseCase
        ArgumentCaptor<CreateNotificationEventCommand> eventCaptor = ArgumentCaptor.forClass(CreateNotificationEventCommand.class);
        verify(createNotificationEventUseCase).execute(eventCaptor.capture());
        CreateNotificationEventCommand savedEvent = eventCaptor.getValue();
        assertEquals("TASK_DUE_REMINDER:101:" + taskDueDate, savedEvent.sourceEventKey());
        assertEquals(List.of(50L), savedEvent.recipientUserIds());
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-01 + QTN-19: Bỏ qua không gửi lại thông báo trùng nếu sự kiện đã từng gửi")
    void tc01_qtn19_doNotSendDuplicateReminder() {
        LocalDate taskDueDate = scanDate.plusDays(2);
        Task task = createTask(101L, 10L, "Xây dựng tính năng đăng nhập", 5L, taskDueDate, TaskStatus.IN_PROGRESS);
        Employee employee = createEmployee(5L, 50L, "Nguyễn Văn Chuyên Môn");

        when(loadTaskDueReminderPort.findTasksDueBetween(scanDate, scanDate.plusDays(3))).thenReturn(List.of(task));
        when(loadEmployeePort.findById(new EmployeeId(5L))).thenReturn(Optional.of(employee));
        // QTN-19: Đã gửi trước đó!
        when(checkTaskDueReminderSentPort.hasReminderBeenSent(new UserId(50L), 101L, taskDueDate)).thenReturn(true);

        TaskDueReminderScanResult result = service.execute(scanDate);

        assertEquals(1, result.totalScanned());
        assertEquals(0, result.sentCount());
        assertEquals(1, result.skippedDuplicateCount()); // Bỏ qua do QTN-19
        assertTrue(result.notifiedTaskIds().isEmpty());

        // Đảm bảo tuyệt đối không gửi lại thông báo trùng
        verify(saveNotificationPort, never()).save(any());
        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-02: Ngoại lệ - Công việc đã hoàn thành (DONE) thì không gửi nhắc")
    void tc02_doNotSendReminder_whenTaskIsCompleted() {
        LocalDate taskDueDate = scanDate.plusDays(2);
        Task completedTask = createTask(102L, 10L, "Viết Unit Test", 5L, taskDueDate, TaskStatus.DONE);

        when(loadTaskDueReminderPort.findTasksDueBetween(scanDate, scanDate.plusDays(3))).thenReturn(List.of(completedTask));

        TaskDueReminderScanResult result = service.execute(scanDate);

        assertEquals(1, result.totalScanned());
        assertEquals(0, result.sentCount());
        assertEquals(1, result.skippedCompletedCount());
        assertTrue(result.notifiedTaskIds().isEmpty());

        verify(saveNotificationPort, never()).save(any());
        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-02: Ngoại lệ - Công việc đã hủy (CANCELLED) thì không gửi nhắc")
    void tc02_doNotSendReminder_whenTaskIsCancelled() {
        LocalDate taskDueDate = scanDate.plusDays(1);
        Task cancelledTask = createTask(103L, 10L, "Nghiên cứu thư viện", 5L, taskDueDate, TaskStatus.CANCELLED);

        when(loadTaskDueReminderPort.findTasksDueBetween(scanDate, scanDate.plusDays(3))).thenReturn(List.of(cancelledTask));

        TaskDueReminderScanResult result = service.execute(scanDate);

        assertEquals(1, result.totalScanned());
        assertEquals(0, result.sentCount());
        assertEquals(1, result.skippedCompletedCount());

        verify(saveNotificationPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-11-CN-004-TC-04: Lưu lịch sử - Ghi nhật ký kiểm toán (Audit Log) khi rà soát hoàn tất")
    void tc04_saveAuditLog_whenScanCompleted() {
        LocalDate taskDueDate = scanDate.plusDays(2);
        Task task = createTask(101L, 10L, "Xây dựng tính năng", 5L, taskDueDate, TaskStatus.TODO);
        Employee employee = createEmployee(5L, 50L, "Nguyễn Văn A");

        when(loadTaskDueReminderPort.findTasksDueBetween(scanDate, scanDate.plusDays(3))).thenReturn(List.of(task));
        when(loadEmployeePort.findById(new EmployeeId(5L))).thenReturn(Optional.of(employee));
        when(checkTaskDueReminderSentPort.hasReminderBeenSent(any(), anyLong(), any())).thenReturn(false);

        service.execute(scanDate);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();

        assertNotNull(savedAudit);
        assertEquals("SCAN_TASK_DUE_REMINDERS", savedAudit.getAction());
        assertEquals("tasks", savedAudit.getTableName());
        assertTrue(savedAudit.getNewValue().contains("sentCount=1"));
        assertTrue(savedAudit.getNewValue().contains("notifiedTaskIds=[101]"));
    }
}
