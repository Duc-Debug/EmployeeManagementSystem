package com.hrm.employeemanagement.application.port.inbound.notification;

public interface DeleteNotificationItemUseCase {
    void deleteNotification(Long recipientId, Long currentUserId);
}
