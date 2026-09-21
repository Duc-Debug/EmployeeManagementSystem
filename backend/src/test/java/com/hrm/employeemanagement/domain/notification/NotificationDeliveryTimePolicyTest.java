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
}
