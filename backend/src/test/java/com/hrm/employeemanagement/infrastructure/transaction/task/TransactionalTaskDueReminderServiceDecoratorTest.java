package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;

class TransactionalTaskDueReminderServiceDecoratorTest {

    @Test
    @DisplayName("Kiểm tra @Transactional trên phương thức executeScanAndSend")
    void scanMethodDefinesTransactionBoundary() throws Exception {
        Method method = TransactionalTaskDueReminderServiceDecorator.class.getMethod("execute", LocalDate.class);
        assertTrue(method.isAnnotationPresent(Transactional.class));
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertEquals(false, transactional.readOnly());
    }

    @Test
    @DisplayName("Kiểm tra @Transactional(readOnly = true) trên phương thức execute cho my-tasks")
    void getMyTasksMethodDefinesReadOnlyTransactionBoundary() throws Exception {
        Method method = TransactionalTaskDueReminderServiceDecorator.class.getMethod("execute");
        assertTrue(method.isAnnotationPresent(Transactional.class));
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
    }

    @Test
    @DisplayName("Decorator ủy quyền chính xác cho ScanAndSendTaskDueRemindersUseCase")
    void delegatesToScanAndSendUseCase() {
        ScanAndSendTaskDueRemindersUseCase scanMock = mock(ScanAndSendTaskDueRemindersUseCase.class);
        LocalDate date = LocalDate.of(2026, 9, 18);
        TaskDueReminderScanResult expected = new TaskDueReminderScanResult(date, 5, 2, 1, 1, List.of(1L, 2L));
        when(scanMock.execute(date)).thenReturn(expected);

        TransactionalTaskDueReminderServiceDecorator decorator =
                new TransactionalTaskDueReminderServiceDecorator(scanMock);

        TaskDueReminderScanResult result = decorator.execute(date);

        assertNotNull(result);
        assertEquals(2, result.sentCount());
        verify(scanMock).execute(date);
    }

    @Test
    @DisplayName("Decorator ủy quyền chính xác cho GetMyUpcomingDueTasksUseCase")
    void delegatesToGetMyUpcomingTasksUseCase() {
        GetMyUpcomingDueTasksUseCase getMock = mock(GetMyUpcomingDueTasksUseCase.class);
        UpcomingDueTaskResult item = new UpcomingDueTaskResult(
                10L, 1L, "TSK-10", "Task", LocalDate.of(2026, 9, 20), 2, "IN_PROGRESS", "/projects/1/tasks/10"
        );
        when(getMock.execute()).thenReturn(List.of(item));

        TransactionalTaskDueReminderServiceDecorator decorator =
                new TransactionalTaskDueReminderServiceDecorator(getMock);

        List<UpcomingDueTaskResult> results = decorator.execute();

        assertEquals(1, results.size());
        assertEquals("TSK-10", results.get(0).taskCode());
        verify(getMock).execute();
    }
}