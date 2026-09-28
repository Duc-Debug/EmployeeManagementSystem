package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.notification.NotificationResult;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationReadUseCase;
import com.hrm.employeemanagement.application.service.notification.NotificationApplicationService;

/**
 * Transaction boundary for notification queries and read-status updates.
 */
public class TransactionalNotificationServiceDecorator
        implements GetNotificationsUseCase, MarkNotificationReadUseCase {

    private final NotificationApplicationService delegate;

    public TransactionalNotificationServiceDecorator(NotificationApplicationService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "NotificationApplicationService không được null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResult> execute(Long recipientUserId) {
        return delegate.execute(recipientUserId);
    }

    @Override
    @Transactional
    public void execute(Long notificationId, Long recipientUserId) {
        delegate.execute(notificationId, recipientUserId);
    }

    @Override
    @Transactional
    public void executeAll(Long recipientUserId) {
        delegate.executeAll(recipientUserId);
    }
}

