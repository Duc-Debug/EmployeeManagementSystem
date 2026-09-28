package com.hrm.employeemanagement.domain.exception.notification;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class NotificationAccessDeniedException extends DomainException {
    public NotificationAccessDeniedException(String message) {
        super(message);
    }

    public NotificationAccessDeniedException(Long notificationRecipientId, Long userId) {
        super(String.format("Người dùng ID: %d không có quyền truy cập thông báo ID: %d", userId, notificationRecipientId));
    }
}
