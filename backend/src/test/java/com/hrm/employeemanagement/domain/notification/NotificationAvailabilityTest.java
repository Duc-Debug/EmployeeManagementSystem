package com.hrm.employeemanagement.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.user.UserId;

class NotificationAvailabilityTest {
    @Test
    void schedulingPreservesActualCreationTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 21, 10, 0);
        LocalDateTime availableAt = LocalDateTime.of(2026, 9, 21, 17, 0);
        Notification notification = new Notification(
                null, new UserId(1L), null, NotificationType.TASK_COMMENT,
                "TASK", 10L, "Title", "Content", false, createdAt);

        Notification scheduled = notification.scheduledFor(availableAt);

        assertEquals(createdAt, scheduled.getCreatedAt());
        assertEquals(availableAt, scheduled.getAvailableAt());
    }
}
