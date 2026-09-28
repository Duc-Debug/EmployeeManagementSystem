package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationId;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;

@Component
public class NotificationPersistenceMapper {

    public Notification toDomain(NotificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationType type = NotificationType.TASK_MENTION;
        try {
            if (entity.getType() != null) {
                type = NotificationType.valueOf(entity.getType());
            }
        } catch (IllegalArgumentException ignored) {
        }

        return new Notification(
                NotificationId.of(entity.getId()),
                new UserId(entity.getRecipientId()),
                entity.getSenderId() != null ? new UserId(entity.getSenderId()) : null,
                type,
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTitle(),
                entity.getContent(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getAvailableAt());
    }

    public NotificationJpaEntity toJpaEntity(Notification domain) {
        if (domain == null) {
            return null;
        }

        return new NotificationJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getRecipientId().value(),
                domain.getSenderId() != null ? domain.getSenderId().value() : null,
                domain.getType().name(),
                domain.getTargetType(),
                domain.getTargetId(),
                domain.getTitle(),
                domain.getContent(),
                domain.isRead(),
                domain.getCreatedAt(),
                domain.getAvailableAt());
    }
}

