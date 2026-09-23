package com.hrm.employeemanagement.domain.exception.notification;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class NotificationPreferenceValidationException extends DomainException {
    public NotificationPreferenceValidationException(String message) {
        super(message);
    }
}
