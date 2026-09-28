package com.hrm.employeemanagement.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class NotificationDeliveryTimePolicyTest {

    @Test
    void dailyDigestReleasesAtNextFivePm() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 10, 0);
        assertEquals(LocalDateTime.of(2026, 9, 21, 17, 0),
                NotificationDeliveryTimePolicy.releaseAt(
                        NotificationFrequency.DAILY_DIGEST, NotificationType.TASK_ASSIGNED, now));
    }

    @Test
    void weeklyDigestReleasesOnNextMondayMorning() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 22, 10, 0);
        assertEquals(LocalDateTime.of(2026, 9, 28, 9, 0),
                NotificationDeliveryTimePolicy.releaseAt(
                        NotificationFrequency.WEEKLY_DIGEST, NotificationType.TASK_COMMENT, now));
    }

    @Test
    void criticalNotificationsAreAlwaysImmediate() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 23, 0);
        assertEquals(now, NotificationDeliveryTimePolicy.releaseAt(
                NotificationFrequency.WEEKLY_DIGEST, NotificationType.SCHEDULE_CONFLICT, now));
        assertEquals(now, NotificationDeliveryTimePolicy.releaseAt(
                NotificationFrequency.DAILY_DIGEST, NotificationType.ALLOCATION_CHANGED, now));
    }

    @Test
    void criticalEmailBypassesQuietHoursWhileNormalEmailUsesItsNextDigestWindow() {
        NotificationPreference preference = NotificationPreference.createDefault(
                new com.hrm.employeemanagement.domain.user.UserId(1L));
        preference.update(
                true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.DAILY_DIGEST, 3,
                QuietHours.of(true, java.time.LocalTime.of(22, 0), java.time.LocalTime.of(7, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 22, 30);

        assertEquals(now, NotificationDeliveryTimePolicy.decide(
                preference, NotificationType.SCHEDULE_CONFLICT, true, now).availableAt());
        assertEquals(LocalDateTime.of(2026, 9, 22, 17, 0), NotificationDeliveryTimePolicy.decide(
                preference, NotificationType.TASK_COMMENT, true, now).availableAt());
    }

    @Test
    void exactDailyCutoffBelongsToNextBatch() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 9, 21, 17, 0);
        assertEquals(cutoff.plusDays(1), NotificationDeliveryTimePolicy.releaseAt(
                NotificationFrequency.DAILY_DIGEST, NotificationType.TASK_COMMENT, cutoff));
    }

    @Test
    void exactWeeklyCutoffBelongsToNextBatch() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 9, 21, 9, 0);
        assertEquals(cutoff.plusWeeks(1), NotificationDeliveryTimePolicy.releaseAt(
                NotificationFrequency.WEEKLY_DIGEST, NotificationType.TASK_COMMENT, cutoff));
    }

    @Test
    void quietHoursDelayEmailButNotInAppNotification() {
        NotificationPreference preference = NotificationPreference.createDefault(
                new com.hrm.employeemanagement.domain.user.UserId(1L));
        preference.update(
                true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE, 3,
                QuietHours.of(true, java.time.LocalTime.of(22, 0), java.time.LocalTime.of(7, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 22, 30);

        NotificationDeliveryDecision email = NotificationDeliveryTimePolicy.decide(
                preference, NotificationType.TASK_COMMENT, true, now);
        NotificationDeliveryDecision inApp = NotificationDeliveryTimePolicy.decide(
                preference, NotificationType.TASK_COMMENT, false, now);

        assertEquals(LocalDateTime.of(2026, 9, 22, 7, 0), email.availableAt());
        assertEquals(now, inApp.availableAt());
    }

    @Test
    void dailyDigestInsideQuietHoursMovesToQuietHoursEnd() {
        NotificationPreference preference = NotificationPreference.createDefault(
                new com.hrm.employeemanagement.domain.user.UserId(1L));
        preference.update(
                true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.DAILY_DIGEST, 3,
                QuietHours.of(true, java.time.LocalTime.of(16, 0), java.time.LocalTime.of(18, 0)));

        NotificationDeliveryDecision decision = NotificationDeliveryTimePolicy.decide(
                preference, NotificationType.TASK_ASSIGNED, true,
                LocalDateTime.of(2026, 9, 21, 10, 0));

        assertEquals(LocalDateTime.of(2026, 9, 21, 18, 0), decision.availableAt());
    }
}
