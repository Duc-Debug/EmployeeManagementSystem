package com.hrm.employeemanagement.domain.notification;

import java.util.Objects;

public record NotificationRecipientId(Long value) {
    public NotificationRecipientId {
        Objects.requireNonNull(value, "NotificationRecipientId không được null");
    }

    public static NotificationRecipientId of(Long value) {
        return value != null ? new NotificationRecipientId(value) : null;
    }
}
