package com.hrm.employeemanagement.domain.notification;

import java.util.Objects;

public record NotificationPreferenceId(Long value) {
    public NotificationPreferenceId {
        Objects.requireNonNull(value, "NotificationPreferenceId không được null");
    }

    public static NotificationPreferenceId of(Long value) {
        return value != null ? new NotificationPreferenceId(value) : null;
    }
}
