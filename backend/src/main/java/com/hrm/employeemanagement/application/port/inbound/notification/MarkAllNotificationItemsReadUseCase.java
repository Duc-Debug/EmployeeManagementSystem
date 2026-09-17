package com.hrm.employeemanagement.application.port.inbound.notification;

public interface MarkAllNotificationItemsReadUseCase {
    void markAllAsRead(Long currentUserId);
}
