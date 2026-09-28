package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;

public interface CreateNotificationEventUseCase {
    Long execute(CreateNotificationEventCommand command);
}
