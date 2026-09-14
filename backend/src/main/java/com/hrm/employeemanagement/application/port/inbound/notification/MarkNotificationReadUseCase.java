package com.hrm.employeemanagement.application.port.inbound.notification;

public interface MarkNotificationReadUseCase {
    void execute(Long notificationId, Long recipientUserId);

    void executeAll(Long recipientUserId);
}

