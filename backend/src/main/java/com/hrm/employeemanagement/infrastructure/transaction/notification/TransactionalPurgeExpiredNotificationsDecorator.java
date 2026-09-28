package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.inbound.notification.PurgeExpiredNotificationsUseCase;
import com.hrm.employeemanagement.application.service.notification.PurgeExpiredNotificationsService;

public class TransactionalPurgeExpiredNotificationsDecorator implements PurgeExpiredNotificationsUseCase {

    private final PurgeExpiredNotificationsService delegate;

    public TransactionalPurgeExpiredNotificationsDecorator(PurgeExpiredNotificationsService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "PurgeExpiredNotificationsService must not be null");
    }

    @Override
    @Transactional
    public long execute() {
        return delegate.execute();
    }
}
