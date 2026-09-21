package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.ResetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.UpdateNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.service.notification.NotificationPreferenceApplicationService;

public class TransactionalNotificationPreferenceDecorator implements
        GetNotificationPreferenceUseCase,
        UpdateNotificationPreferenceUseCase,
        ResetNotificationPreferenceUseCase {

    private final NotificationPreferenceApplicationService delegate;

    public TransactionalNotificationPreferenceDecorator(NotificationPreferenceApplicationService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "NotificationPreferenceApplicationService must not be null");
    }

    @Override
    @Transactional
    public NotificationPreferenceResult getMyPreference(Long currentUserId) {
        return delegate.getMyPreference(currentUserId);
    }

    @Override
    @Transactional
    public NotificationPreferenceResult updateMyPreference(Long currentUserId, UpdateNotificationPreferenceCommand command) {
        return delegate.updateMyPreference(currentUserId, command);
    }

    @Override
    @Transactional
    public NotificationPreferenceResult resetMyPreference(Long currentUserId) {
        return delegate.resetMyPreference(currentUserId);
    }
}
