package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

class TaskDueReminderControllerTest {

    private GetMyUpcomingDueTasksUseCase getMyUpcomingDueTasksUseCase;
    private ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase;
    private TaskDueReminderController controller;

    @BeforeEach
    void setUp() {
        getMyUpcomingDueTasksUseCase = mock(GetMyUpcomingDueTasksUseCase.class);
        scanAndSendTaskDueRemindersUseCase = mock(ScanAndSendTaskDueRemindersUseCase.class);
        controller = new TaskDueReminderController(getMyUpcomingDueTasksUseCase, scanAndSendTaskDueRemindersUseCase);
    }

    @Test
    @DisplayName("GET /api/v1/tasks/due-reminders/my-tasks: Trả về HTTP 200 với danh sách công việc")
    void getMyUpcomingDueTasks_returnsOk() {
        UpcomingDueTaskResult task = new UpcomingDueTaskResult(
                1L, 10L, "TSK-01", "Task 1", LocalDate.now().plusDays(2), 2, "IN_PROGRESS", "/projects/10/tasks/1"
        );
        when(getMyUpcomingDueTasksUseCase.execute()).thenReturn(List.of(task));

        ResponseEntity<ApiResponse<List<UpcomingDueTaskResult>>> response = controller.getMyUpcomingDueTasks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Task 1", response.getBody().getData().get(0).taskName());
        verify(getMyUpcomingDueTasksUseCase).execute();
    }

    @Test
    @DisplayName("POST /api/v1/tasks/due-reminders/scan: Trả về HTTP 200 với kết quả rà soát")
    void scanAndSendReminders_returnsOk() {
        LocalDate date = LocalDate.of(2026, 9, 18);
        TaskDueReminderScanResult scanResult = new TaskDueReminderScanResult(date, 5, 2, 1, 1, List.of(10L, 11L));
        when(scanAndSendTaskDueRemindersUseCase.execute(date)).thenReturn(scanResult);

        ResponseEntity<ApiResponse<TaskDueReminderScanResult>> response = controller.scanAndSendReminders(date);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(2, response.getBody().getData().sentCount());
        assertEquals(1, response.getBody().getData().skippedDuplicateCount());
        verify(scanAndSendTaskDueRemindersUseCase).execute(date);
    }
}
