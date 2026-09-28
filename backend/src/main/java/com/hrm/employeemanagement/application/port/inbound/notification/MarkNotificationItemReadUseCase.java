package com.hrm.employeemanagement.application.port.inbound.notification;

public interface MarkNotificationItemReadUseCase {
    void markAsRead(Long recipientId, Long currentUserId);
}
