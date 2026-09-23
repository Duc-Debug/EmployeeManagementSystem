package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.application.port.inbound.notification.PurgeExpiredNotificationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;

/**
 * Service định kỳ dọn dẹp (hard delete) các thông báo quá 45 ngày tính từ notification_event.created_at.
 * Áp dụng cho thông báo đã đọc, chưa đọc và đã soft-delete (TC-05).
 */
public class PurgeExpiredNotificationsService implements PurgeExpiredNotificationsUseCase {

    private static final int RETENTION_DAYS = 45;

    private final NotificationRecipientRepositoryPort recipientRepositoryPort;
    private final NotificationEventRepositoryPort eventRepositoryPort;

    public PurgeExpiredNotificationsService(
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            NotificationEventRepositoryPort eventRepositoryPort
    ) {
        this.recipientRepositoryPort = Objects.requireNonNull(recipientRepositoryPort, "recipientRepositoryPort must not be null");
        this.eventRepositoryPort = Objects.requireNonNull(eventRepositoryPort, "eventRepositoryPort must not be null");
    }

    @Override
    public long execute() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        // 1. Hard delete toàn bộ recipient items mà event.created_at < cutoff
        long purgedRecipients = recipientRepositoryPort.purgeRecipientsOlderThan(cutoff);

        // 2. Dọn dẹp các event mồ côi (không còn recipient nào tham chiếu và created_at < cutoff)
        long purgedEvents = eventRepositoryPort.purgeOrphanEventsOlderThan(cutoff);

        return purgedRecipients + purgedEvents;
    }
}
