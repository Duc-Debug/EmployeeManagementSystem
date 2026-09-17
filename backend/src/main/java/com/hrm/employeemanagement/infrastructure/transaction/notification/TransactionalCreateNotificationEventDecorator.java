package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.service.notification.CreateNotificationEventService;

public class TransactionalCreateNotificationEventDecorator implements CreateNotificationEventUseCase {

    private final CreateNotificationEventService delegate;

    public TransactionalCreateNotificationEventDecorator(CreateNotificationEventService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "CreateNotificationEventService must not be null");
    }

    @Override
    @Transactional
    public Long execute(CreateNotificationEventCommand command) {
        return delegate.execute(command);
    }
}
