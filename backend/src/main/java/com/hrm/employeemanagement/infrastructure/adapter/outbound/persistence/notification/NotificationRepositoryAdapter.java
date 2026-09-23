package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationId;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRepository;

@Component
public class NotificationRepositoryAdapter implements LoadNotificationPort, SaveNotificationPort {

    private final SpringDataNotificationRepository notificationRepository;
    private final NotificationPersistenceMapper mapper;
    private final TransactionalLegacyDigestHelper digestHelper;

    public NotificationRepositoryAdapter(
            SpringDataNotificationRepository notificationRepository,
            NotificationPersistenceMapper mapper,
            TransactionalLegacyDigestHelper digestHelper) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository không được null");
        this.mapper = Objects.requireNonNull(mapper, "NotificationPersistenceMapper không được null");
        this.digestHelper = Objects.requireNonNull(digestHelper, "digestHelper must not be null");
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return notificationRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findAllByRecipientId(UserId recipientId) {
        if (recipientId == null || recipientId.value() == null) {
            return List.of();
        }
        return notificationRepository.findReleasedByRecipientId(recipientId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = mapper.toJpaEntity(notification);
        NotificationJpaEntity saved = notificationRepository.save(entity);
        return mapper.toDomain(saved);
    }

    public Notification appendToDigest(
            Notification notification,
            java.time.LocalDateTime availableAt,
            NotificationFrequency frequency,
            java.time.ZoneId zone
    ) {
        long batchId = availableAt.atZone(zone).toInstant().toEpochMilli();
        String item = "• " + notification.getTitle()
                + (notification.getContent() == null || notification.getContent().isBlank()
                        ? "" : ": " + notification.getContent());
        var existing = notificationRepository
                .findFirstByRecipientIdAndTypeAndTargetIdAndAvailableAt(
                        notification.getRecipientId().value(),
                        NotificationType.NOTIFICATION_DIGEST.name(), batchId, availableAt);
        if (existing.isPresent()) {
            NotificationJpaEntity entity = existing.get();
            digestHelper.append(entity.getId(), item);
            return mapper.toDomain(notificationRepository.findById(entity.getId()).orElseThrow());
        }
        Notification digest = new Notification(
                null, notification.getRecipientId(), null, NotificationType.NOTIFICATION_DIGEST,
                "NOTIFICATION_DIGEST", batchId,
                frequency == NotificationFrequency.DAILY_DIGEST
                        ? "Bản tin thông báo hàng ngày" : "Bản tin thông báo hàng tuần",
                item, false, notification.getCreatedAt(), availableAt);
        try {
            return mapper.toDomain(digestHelper.create(mapper.toJpaEntity(digest)));
        } catch (DataIntegrityViolationException concurrentInsert) {
            NotificationJpaEntity winner = notificationRepository
                    .findFirstByRecipientIdAndTypeAndTargetIdAndAvailableAt(
                            notification.getRecipientId().value(),
                            NotificationType.NOTIFICATION_DIGEST.name(), batchId, availableAt)
                    .orElseThrow(() -> concurrentInsert);
            digestHelper.append(winner.getId(), item);
            return mapper.toDomain(notificationRepository.findById(winner.getId()).orElseThrow());
        }
    }

    @Override
    public void markAllAsRead(UserId recipientId) {
        if (recipientId != null && recipientId.value() != null) {
            notificationRepository.markAllAsReadByRecipientId(recipientId.value());
        }
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        List<NotificationJpaEntity> entities = notifications.stream()
                .map(mapper::toJpaEntity)
                .toList();
        notificationRepository.saveAll(entities);
    }
}

