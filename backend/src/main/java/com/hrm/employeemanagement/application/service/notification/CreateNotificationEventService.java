package com.hrm.employeemanagement.application.service.notification;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryTimePolicy;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Service tạo sự kiện thông báo nội bộ tuân thủ quy tắc QTN-19:
 * 1. source_event_key đại diện cho business event duy nhất.
 * 2. Idempotent: gọi lại không tạo duplicate event hay duplicate recipient.
 * 3. Nếu recipient đã tồn tại và đã xóa mềm (is_deleted = true), hệ thống KHÔNG tạo mới và KHÔNG tự ý restore.
 */
public class CreateNotificationEventService implements CreateNotificationEventUseCase {

    private final NotificationEventRepositoryPort eventRepositoryPort;
    private final NotificationRecipientRepositoryPort recipientRepositoryPort;
    private final LoadNotificationPreferencePort preferencePort;

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort
    ) {
        this(eventRepositoryPort, recipientRepositoryPort, null);
    }

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            LoadNotificationPreferencePort preferencePort
    ) {
        this.eventRepositoryPort = Objects.requireNonNull(eventRepositoryPort, "eventRepositoryPort must not be null");
        this.recipientRepositoryPort = Objects.requireNonNull(recipientRepositoryPort, "recipientRepositoryPort must not be null");
        this.preferencePort = preferencePort;
    }

    @Override
    public Long execute(CreateNotificationEventCommand command) {
        Objects.requireNonNull(command, "CreateNotificationEventCommand must not be null");

        NotificationEvent newEvent = NotificationEvent.create(
                command.eventType(),
                command.level(),
                command.title(),
                command.message(),
                command.relatedEntityType(),
                command.relatedEntityId(),
                command.sourceEventKey()
        );

        // Nhận hoặc tạo mới event an toàn xử lý race condition
        NotificationEvent event = eventRepositoryPort.getOrCreate(newEvent);

        // Fan-out tới danh sách người nhận theo QTN-19
        if (command.recipientUserIds() != null) {
            for (Long recipientUserIdVal : command.recipientUserIds()) {
                if (recipientUserIdVal == null) {
                    continue;
                }
                UserId recipientUserId = new UserId(recipientUserIdVal);
                if (!isInAppEnabled(recipientUserId, command.eventType())) {
                    continue;
                }

                var existingRecipientOpt = recipientRepositoryPort.findByEventIdAndRecipientUserId(
                        event.getId(),
                        recipientUserId
                );

                if (existingRecipientOpt.isEmpty()) {
                    // Chưa từng có bản ghi -> Thêm mới với cơ chế saveIfAbsent an toàn đồng thời (idempotent)
                    NotificationRecipientItem newRecipient = createRecipient(event.getId(), recipientUserId, typeOf(command.eventType()));
                    recipientRepositoryPort.saveIfAbsent(newRecipient);
                }
                // Nếu đã tồn tại:
                // - Dù is_deleted = false hay is_deleted = true, theo phương án A đã chốt:
                //   Không làm gì, không tạo mới, không tự động khôi phục.
            }
        }

        return event.getId().value();
    }

    private boolean isInAppEnabled(UserId recipientUserId, String eventType) {
        if (preferencePort == null) {
            return true;
        }
        NotificationType type;
        try {
            type = NotificationType.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return true;
        }
        NotificationPreference preference = preferencePort.findByUserId(recipientUserId)
                .orElseGet(() -> NotificationPreference.createDefault(recipientUserId));
        return preference.isChannelActiveFor(type, false, java.time.LocalTime.now());
    }

    private NotificationRecipientItem createRecipient(
            com.hrm.employeemanagement.domain.notification.NotificationEventId eventId,
            UserId recipientUserId,
            NotificationType type
    ) {
        NotificationPreference preference = preferencePort == null
                ? NotificationPreference.createDefault(recipientUserId)
                : preferencePort.findByUserId(recipientUserId)
                        .orElseGet(() -> NotificationPreference.createDefault(recipientUserId));
        java.time.LocalDateTime releaseAt = NotificationDeliveryTimePolicy.releaseAt(
                preference.getFrequency(), type, java.time.LocalDateTime.now());
        return new NotificationRecipientItem(
                null, eventId, recipientUserId, false, null, false, null, releaseAt);
    }

    private NotificationType typeOf(String eventType) {
        try {
            return NotificationType.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }
}
