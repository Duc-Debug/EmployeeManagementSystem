package com.hrm.employeemanagement.application.port.outbound.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.user.UserId;

public interface NotificationRecipientRepositoryPort {
    NotificationRecipientItem save(NotificationRecipientItem item);
    NotificationRecipientItem saveIfAbsent(NotificationRecipientItem item);
    Optional<NotificationRecipientItem> findById(NotificationRecipientId id);
    Optional<NotificationRecipientItem> findByEventIdAndRecipientUserId(NotificationEventId eventId, UserId recipientUserId);
    List<NotificationRecipientItem> findRecipients(UserId recipientUserId, String status, String level, int page, int size);
    long countRecipients(UserId recipientUserId, String status, String level);
    List<NotificationRecipientItem> findRecipientsByEventType(UserId recipientUserId, String status, String level, String eventType, int page, int size);
    long countRecipientsByEventType(UserId recipientUserId, String status, String level, String eventType);
    long countUnread(UserId recipientUserId);
    int markAllAsRead(UserId recipientUserId, LocalDateTime now);
    long purgeRecipientsOlderThan(LocalDateTime cutoff);
}
