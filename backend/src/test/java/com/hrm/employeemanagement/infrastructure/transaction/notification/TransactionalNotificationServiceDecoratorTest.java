package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.notification.NotificationResult;
import com.hrm.employeemanagement.application.service.notification.NotificationApplicationService;

class TransactionalNotificationServiceDecoratorTest {

    @Test
    void executeGetHasTransactionalReadOnlyAnnotation() throws NoSuchMethodException {
        Method method = TransactionalNotificationServiceDecorator.class.getMethod("execute", Long.class);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertTrue(annotation != null && annotation.readOnly());
    }

    @Test
    void executeMarkReadHasTransactionalAnnotation() throws NoSuchMethodException {
        Method method = TransactionalNotificationServiceDecorator.class.getMethod("execute", Long.class, Long.class);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertTrue(annotation != null && !annotation.readOnly());
    }

    @Test
    void delegatesExecutionToApplicationService() {
        NotificationApplicationService delegate = mock(NotificationApplicationService.class);
        TransactionalNotificationServiceDecorator decorator = new TransactionalNotificationServiceDecorator(delegate);

        List<NotificationResult> expected = List.of();
        when(delegate.execute(5L)).thenReturn(expected);

        List<NotificationResult> result = decorator.execute(5L);
        assertEquals(expected, result);
        verify(delegate).execute(5L);

        decorator.execute(10L, 5L);
        verify(delegate).execute(10L, 5L);

        decorator.executeAll(5L);
        verify(delegate).executeAll(5L);
    }
}

