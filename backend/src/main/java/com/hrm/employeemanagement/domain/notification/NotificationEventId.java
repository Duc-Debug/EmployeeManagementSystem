package com.hrm.employeemanagement.domain.notification;

import java.util.Objects;

public record NotificationEventId(Long value) {
    public NotificationEventId {
        Objects.requireNonNull(value, "NotificationEventId không được null");
    }

    public static NotificationEventId of(Long value) {
        return value != null ? new NotificationEventId(value) : null;
    }
}
