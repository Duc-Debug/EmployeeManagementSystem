package com.hrm.employeemanagement.application.service.notification;

import java.util.Objects;
import java.time.Clock;
import java.time.LocalDateTime;

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
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryDecision;
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
    private final Clock clock;

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort
    ) {
        this(eventRepositoryPort, recipientRepositoryPort, null, Clock.systemDefaultZone());
    }

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            LoadNotificationPreferencePort preferencePort
    ) {
        this(eventRepositoryPort, recipientRepositoryPort, preferencePort, Clock.systemDefaultZone());
    }

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort,
            LoadNotificationPreferencePort preferencePort,
            Clock clock
    ) {
        this.eventRepositoryPort = Objects.requireNonNull(eventRepositoryPort, "eventRepositoryPort must not be null");
        this.recipientRepositoryPort = Objects.requireNonNull(recipientRepositoryPort, "recipientRepositoryPort must not be null");
        this.preferencePort = preferencePort;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Long execute(CreateNotificationEventCommand command) {
        Objects.requireNonNull(command, "CreateNotificationEventCommand must not be null");

        NotificationEvent newEvent = new NotificationEvent(
                null,
                command.eventType(),
                command.level(),
                command.title(),
                command.message(),
                command.relatedEntityType(),
                command.relatedEntityId(),
                command.sourceEventKey(),
                LocalDateTime.now(clock)
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
                NotificationPreference preference = loadPreference(recipientUserId);
                NotificationType notificationType = typeOf(command.eventType());
                NotificationDeliveryDecision decision = NotificationDeliveryTimePolicy.decide(
                        preference, notificationType, false, LocalDateTime.now(clock));
                if (!decision.enabled()) {
                    continue;
                }

                NotificationEvent recipientEvent = decision.isDigest()
                        ? appendToDigestEvent(command, recipientUserId, decision)
                        : event;
                var existingRecipientOpt = recipientRepositoryPort.findByEventIdAndRecipientUserId(
                        recipientEvent.getId(),
                        recipientUserId
                );

                if (existingRecipientOpt.isEmpty()) {
                    // Chưa từng có bản ghi -> Thêm mới với cơ chế saveIfAbsent an toàn đồng thời (idempotent)
                    NotificationRecipientItem newRecipient = createRecipient(
                            recipientEvent.getId(), recipientUserId, decision.availableAt());
                    recipientRepositoryPort.saveIfAbsent(newRecipient);
                }
                // Nếu đã tồn tại:
                // - Dù is_deleted = false hay is_deleted = true, theo phương án A đã chốt:
                //   Không làm gì, không tạo mới, không tự động khôi phục.
            }
        }

        return event.getId().value();
    }

    private NotificationRecipientItem createRecipient(
            com.hrm.employeemanagement.domain.notification.NotificationEventId eventId,
            UserId recipientUserId,
            LocalDateTime availableAt
    ) {
        LocalDateTime createdAt = LocalDateTime.now(clock);
        return new NotificationRecipientItem(
                null, eventId, recipientUserId, false, null, false, null, createdAt, availableAt);
    }

    private NotificationPreference loadPreference(UserId recipientUserId) {
        return preferencePort == null
                ? NotificationPreference.createDefault(recipientUserId)
                : preferencePort.findByUserId(recipientUserId)
                        .orElseGet(() -> NotificationPreference.createDefault(recipientUserId));
    }

    private NotificationEvent appendToDigestEvent(
            CreateNotificationEventCommand command,
            UserId recipientUserId,
            NotificationDeliveryDecision decision
    ) {
        String key = "notification-digest:" + recipientUserId.value() + ":"
                + decision.frequency().name() + ":" + decision.availableAt();
        String item = "• " + command.title()
                + (command.message() == null || command.message().isBlank() ? "" : ": " + command.message());
        NotificationEvent digestEvent = eventRepositoryPort.getOrCreate(new NotificationEvent(
                null,
                NotificationType.NOTIFICATION_DIGEST.name(),
                com.hrm.employeemanagement.domain.notification.NotificationLevel.THAP,
                decision.frequency() == com.hrm.employeemanagement.domain.notification.NotificationFrequency.DAILY_DIGEST
                        ? "Bản tin thông báo hàng ngày" : "Bản tin thông báo hàng tuần",
                "", "NOTIFICATION_DIGEST", String.valueOf(recipientUserId.value()), key,
                LocalDateTime.now(clock)));
        return eventRepositoryPort.appendDigestItemIfAbsent(
                digestEvent, command.sourceEventKey(), item, LocalDateTime.now(clock));
    }

    private NotificationType typeOf(String eventType) {
        try {
            return NotificationType.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }
}
