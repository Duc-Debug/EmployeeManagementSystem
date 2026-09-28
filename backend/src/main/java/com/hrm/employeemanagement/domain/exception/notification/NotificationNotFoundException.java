package com.hrm.employeemanagement.domain.exception.notification;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class NotificationNotFoundException extends DomainException {
    public NotificationNotFoundException(Long id) {
        super("Không tìm thấy thông báo với ID: " + id);
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }
}
