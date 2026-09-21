package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;

public record NotificationDeliveryDecision(
        boolean enabled,
        LocalDateTime availableAt,
        NotificationFrequency frequency
) {
    public static NotificationDeliveryDecision skip() {
        return new NotificationDeliveryDecision(false, null, NotificationFrequency.IMMEDIATE);
    }

    public boolean isDigest() {
        return frequency == NotificationFrequency.DAILY_DIGEST
                || frequency == NotificationFrequency.WEEKLY_DIGEST;
    }
}
