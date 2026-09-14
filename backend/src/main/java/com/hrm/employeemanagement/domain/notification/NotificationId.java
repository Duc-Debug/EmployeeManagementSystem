package com.hrm.employeemanagement.domain.notification;

import java.util.Objects;

public record NotificationId(Long value) {
    public NotificationId {
        Objects.requireNonNull(value, "NotificationId không được null");
    }

    public static NotificationId of(Long value) {
        return value != null ? new NotificationId(value) : null;
    }
}

