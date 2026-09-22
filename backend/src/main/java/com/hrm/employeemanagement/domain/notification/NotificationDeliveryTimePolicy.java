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

    public static NotificationDeliveryDecision decide(
            NotificationPreference preference,
            NotificationType type,
            boolean email,
            LocalDateTime now
    ) {
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();
        NotificationPreference effectivePreference = java.util.Objects.requireNonNull(preference);
        NotificationDeliveryChannel channel = effectivePreference.getDeliveryChannelFor(type);
        boolean channelEnabled = email
                ? effectivePreference.isEmailEnabled() && channel.isEmailEnabled()
                : effectivePreference.isInAppEnabled() && channel.isInAppEnabled();
        if (!channelEnabled) {
            return NotificationDeliveryDecision.skip();
        }
        if (isCritical(type)) {
            return new NotificationDeliveryDecision(true, effectiveNow, NotificationFrequency.IMMEDIATE);
        }

        NotificationFrequency frequency = effectivePreference.getFrequency();
        LocalDateTime availableAt = releaseAt(frequency, type, effectiveNow);
        if (email && effectivePreference.getQuietHours().isInQuietHours(availableAt.toLocalTime())) {
            availableAt = endOfQuietHours(effectivePreference.getQuietHours(), availableAt);
        }
        return new NotificationDeliveryDecision(true, availableAt, frequency);
    }

    private static LocalDateTime endOfQuietHours(QuietHours quietHours, LocalDateTime time) {
        LocalTime start = quietHours.startTime();
        LocalTime end = quietHours.endTime();
        if (start.isBefore(end)) {
            return time.toLocalDate().atTime(end);
        }
        if (!time.toLocalTime().isBefore(start)) {
            return time.toLocalDate().plusDays(1).atTime(end);
        }
        return time.toLocalDate().atTime(end);
    }

    private static boolean isCritical(NotificationType type) {
        return type == NotificationType.SCHEDULE_CONFLICT || type == NotificationType.ALLOCATION_CHANGED;
    }
}
