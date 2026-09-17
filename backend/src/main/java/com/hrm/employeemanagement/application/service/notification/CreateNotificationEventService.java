package com.hrm.employeemanagement.application.service.notification;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
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

    public CreateNotificationEventService(
            NotificationEventRepositoryPort eventRepositoryPort,
            NotificationRecipientRepositoryPort recipientRepositoryPort
    ) {
        this.eventRepositoryPort = Objects.requireNonNull(eventRepositoryPort, "eventRepositoryPort must not be null");
        this.recipientRepositoryPort = Objects.requireNonNull(recipientRepositoryPort, "recipientRepositoryPort must not be null");
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

                var existingRecipientOpt = recipientRepositoryPort.findByEventIdAndRecipientUserId(
                        event.getId(),
                        recipientUserId
                );

                if (existingRecipientOpt.isEmpty()) {
                    // Chưa từng có bản ghi -> Thêm mới với cơ chế saveIfAbsent an toàn đồng thời (idempotent)
                    NotificationRecipientItem newRecipient = NotificationRecipientItem.create(
                            event.getId(),
                            recipientUserId
                    );
                    recipientRepositoryPort.saveIfAbsent(newRecipient);
                }
                // Nếu đã tồn tại:
                // - Dù is_deleted = false hay is_deleted = true, theo phương án A đã chốt:
                //   Không làm gì, không tạo mới, không tự động khôi phục.
            }
        }

        return event.getId().value();
    }
}
