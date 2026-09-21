package com.hrm.employeemanagement.domain.notification;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public final class NotificationDeliveryTimePolicy {
    private static final LocalTime DAILY_DIGEST_TIME = LocalTime.of(17, 0);
    private static final LocalTime WEEKLY_DIGEST_TIME = LocalTime.of(9, 0);

    private NotificationDeliveryTimePolicy() {
    }

    public static LocalDateTime releaseAt(
            NotificationFrequency frequency,
            NotificationType type,
            LocalDateTime now
    ) {
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();
        if (frequency == null || frequency == NotificationFrequency.IMMEDIATE || isCritical(type)) {
            return effectiveNow;
        }
        if (frequency == NotificationFrequency.DAILY_DIGEST) {
            LocalDateTime today = effectiveNow.toLocalDate().atTime(DAILY_DIGEST_TIME);
            return effectiveNow.isBefore(today) ? today : today.plusDays(1);
        }
        LocalDateTime nextMonday = effectiveNow.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                .atTime(WEEKLY_DIGEST_TIME);
        return effectiveNow.isBefore(nextMonday) ? nextMonday : nextMonday.plusWeeks(1);
    }

    private static boolean isCritical(NotificationType type) {
        return type == NotificationType.SCHEDULE_CONFLICT || type == NotificationType.ALLOCATION_CHANGED;
    }
}
