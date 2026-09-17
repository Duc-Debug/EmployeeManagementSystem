package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.notification.GetNotificationCenterQuery;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterItemResult;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterPageResult;
import com.hrm.employeemanagement.application.dto.notification.UnreadNotificationCountResult;
import com.hrm.employeemanagement.application.port.inbound.notification.DeleteNotificationItemUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationCenterUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetUnreadNotificationCountUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkAllNotificationItemsReadUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationItemReadUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationAuditLogRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException;
import com.hrm.employeemanagement.domain.exception.notification.NotificationNotFoundException;
import com.hrm.employeemanagement.domain.notification.NotificationAuditAction;
import com.hrm.employeemanagement.domain.notification.NotificationAuditLog;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.user.UserId;

public class NotificationCenterApplicationService implements
        GetNotificationCenterUseCase,
        GetUnreadNotificationCountUseCase,
        GetNotificationDetailUseCase,
        MarkNotificationItemReadUseCase,
        MarkAllNotificationItemsReadUseCase,
        DeleteNotificationItemUseCase {

    private final NotificationRecipientRepositoryPort recipientRepositoryPort;
    private final NotificationEventRepositoryPort eventRepositoryPort;
    private final NotificationAuditLogRepositoryPort auditLogRepositoryPort;

    public NotificationCenterApplicationService(
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationAuditLogRepositoryPort auditLogRepositoryPort
    ) {
        this.recipientRepositoryPort = Objects.requireNonNull(recipientRepositoryPort, "recipientRepositoryPort must not be null");
        this.eventRepositoryPort = Objects.requireNonNull(eventRepositoryPort, "eventRepositoryPort must not be null");
        this.auditLogRepositoryPort = Objects.requireNonNull(auditLogRepositoryPort, "auditLogRepositoryPort must not be null");
    }

    @Override
    public NotificationCenterPageResult getNotifications(Long currentUserId, GetNotificationCenterQuery query) {
        validateUserId(currentUserId);
        UserId userId = new UserId(currentUserId);

        List<NotificationRecipientItem> recipients = recipientRepositoryPort.findRecipients(
                userId,
                query.status(),
                query.level(),
                query.page(),
                query.size()
        );
        long totalElements = recipientRepositoryPort.countRecipients(userId, query.status(), query.level());
        long unreadCount = recipientRepositoryPort.countUnread(userId);
        int totalPages = query.size() > 0 ? (int) Math.ceil((double) totalElements / query.size()) : 0;

        List<NotificationEventId> eventIds = recipients.stream()
                .map(NotificationRecipientItem::getEventId)
                .distinct()
                .toList();

        Map<NotificationEventId, NotificationEvent> eventsMap = eventIds.stream()
                .map(eventRepositoryPort::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(NotificationEvent::getId, Function.identity()));

        List<NotificationCenterItemResult> items = new ArrayList<>();
        for (NotificationRecipientItem item : recipients) {
            NotificationEvent event = eventsMap.get(item.getEventId());
            if (event != null) {
                items.add(new NotificationCenterItemResult(
                        item.getId().value(),
                        event.getId().value(),
                        event.getEventType(),
                        event.getLevel(),
                        event.getTitle(),
                        event.getMessage(),
                        event.getRelatedEntityType(),
                        event.getRelatedEntityId(),
                        item.isRead(),
                        item.getReadAt(),
                        item.getCreatedAt()
                ));
            }
        }

        return new NotificationCenterPageResult(
                items,
                totalElements,
                totalPages,
                query.page(),
                query.size(),
                unreadCount
        );
    }

    @Override
    public UnreadNotificationCountResult getUnreadCount(Long currentUserId) {
        validateUserId(currentUserId);
        long count = recipientRepositoryPort.countUnread(new UserId(currentUserId));
        return new UnreadNotificationCountResult(count);
    }

    @Override
    public NotificationCenterItemResult getNotificationDetail(Long recipientId, Long currentUserId) {
        validateRecipientIdAndUserId(recipientId, currentUserId);
        NotificationRecipientItem item = loadAndEnforceOwnership(recipientId, currentUserId);

        NotificationEvent event = eventRepositoryPort.findById(item.getEventId())
                .orElseThrow(() -> new NotificationNotFoundException("Không tìm thấy sự kiện thông báo tương ứng"));

        // Thao tác đọc thuần túy, KHÔNG thay đổi is_read và KHÔNG ghi audit log
        return new NotificationCenterItemResult(
                item.getId().value(),
                event.getId().value(),
                event.getEventType(),
                event.getLevel(),
                event.getTitle(),
                event.getMessage(),
                event.getRelatedEntityType(),
                event.getRelatedEntityId(),
                item.isRead(),
                item.getReadAt(),
                item.getCreatedAt()
        );
    }

    @Override
    public void markAsRead(Long recipientId, Long currentUserId) {
        validateRecipientIdAndUserId(recipientId, currentUserId);
        NotificationRecipientItem item = loadAndEnforceOwnership(recipientId, currentUserId);

        if (!item.isRead()) {
            boolean previousIsRead = item.isRead();
            item.markAsRead(LocalDateTime.now());
            recipientRepositoryPort.save(item);

            String detail = String.format("{\"notificationRecipientId\":%d,\"notificationEventId\":%d,\"previousIsRead\":%s}",
                    item.getId().value(),
                    item.getEventId().value(),
                    previousIsRead);

            NotificationAuditLog auditLog = NotificationAuditLog.create(
                    new UserId(currentUserId),
                    NotificationAuditAction.MARK_READ,
                    "NOTIFICATION_RECIPIENT",
                    item.getId().value(),
                    detail
            );
            auditLogRepositoryPort.save(auditLog);
        }
    }

    @Override
    public void markAllAsRead(Long currentUserId) {
        validateUserId(currentUserId);
        UserId userId = new UserId(currentUserId);
        LocalDateTime now = LocalDateTime.now();

        int affectedCount = recipientRepositoryPort.markAllAsRead(userId, now);

        String detail = String.format("{\"affectedCount\":%d}", affectedCount);
        NotificationAuditLog auditLog = NotificationAuditLog.create(
                userId,
                NotificationAuditAction.MARK_ALL_READ,
                "NOTIFICATION_CENTER",
                null,
                detail
        );
        auditLogRepositoryPort.save(auditLog);
    }

    @Override
    public void deleteNotification(Long recipientId, Long currentUserId) {
        validateRecipientIdAndUserId(recipientId, currentUserId);
        NotificationRecipientItem item = loadAndEnforceOwnership(recipientId, currentUserId);

        NotificationEvent event = eventRepositoryPort.findById(item.getEventId())
                .orElseThrow(() -> new NotificationNotFoundException("Không tìm thấy sự kiện thông báo tương ứng"));

        // Tạo snapshot chi tiết phục vụ tra cứu sau khi xóa mềm
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String receivedAtStr = item.getCreatedAt() != null ? item.getCreatedAt().format(formatter) : "";
        String snapshotJson = String.format(
                "{\"notificationRecipientId\":%d,\"notificationEventId\":%d,\"eventType\":\"%s\",\"level\":\"%s\",\"title\":\"%s\",\"message\":\"%s\",\"relatedEntityType\":%s,\"relatedEntityId\":%s,\"wasRead\":%s,\"receivedAt\":\"%s\"}",
                item.getId().value(),
                event.getId().value(),
                escapeJson(event.getEventType()),
                event.getLevel().name(),
                escapeJson(event.getTitle()),
                escapeJson(event.getMessage()),
                event.getRelatedEntityType() != null ? "\"" + escapeJson(event.getRelatedEntityType()) + "\"" : "null",
                event.getRelatedEntityId() != null ? "\"" + escapeJson(event.getRelatedEntityId()) + "\"" : "null",
                item.isRead(),
                receivedAtStr
        );

        item.softDelete(LocalDateTime.now());
        recipientRepositoryPort.save(item);

        NotificationAuditLog auditLog = NotificationAuditLog.create(
                new UserId(currentUserId),
                NotificationAuditAction.DELETE_NOTIFICATION,
                "NOTIFICATION_RECIPIENT",
                item.getId().value(),
                snapshotJson
        );
        auditLogRepositoryPort.save(auditLog);
    }

    private NotificationRecipientItem loadAndEnforceOwnership(Long recipientId, Long currentUserId) {
        NotificationRecipientItem item = recipientRepositoryPort.findById(NotificationRecipientId.of(recipientId))
                .orElseThrow(() -> new NotificationNotFoundException(recipientId));

        if (item.isDeleted()) {
            throw new NotificationNotFoundException(recipientId);
        }

        if (!item.getRecipientUserId().value().equals(currentUserId)) {
            // Không tiết lộ sự tồn tại của notification thuộc về user khác
            throw new NotificationAccessDeniedException(recipientId, currentUserId);
        }

        return item;
    }

    private void validateUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalStateException("Người dùng chưa được xác thực");
        }
    }

    private void validateRecipientIdAndUserId(Long recipientId, Long currentUserId) {
        validateUserId(currentUserId);
        if (recipientId == null) {
            throw new IllegalArgumentException("Notification Recipient ID không được để trống");
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
