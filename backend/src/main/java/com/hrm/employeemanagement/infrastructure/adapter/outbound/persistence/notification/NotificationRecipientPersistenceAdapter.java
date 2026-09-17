package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationRecipientJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRecipientRepository;

@Component
public class NotificationRecipientPersistenceAdapter implements NotificationRecipientRepositoryPort {

    private final SpringDataNotificationRecipientRepository repository;
    private final TransactionalNotificationRecipientSaveHelper saveHelper;

    public NotificationRecipientPersistenceAdapter(
            SpringDataNotificationRecipientRepository repository,
            TransactionalNotificationRecipientSaveHelper saveHelper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.saveHelper = Objects.requireNonNull(saveHelper, "saveHelper must not be null");
    }

    @Override
    public NotificationRecipientItem save(NotificationRecipientItem item) {
        NotificationRecipientJpaEntity entity = toJpaEntity(item);
        NotificationRecipientJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public NotificationRecipientItem saveIfAbsent(NotificationRecipientItem item) {
        if (item == null) {
            return null;
        }
        Optional<NotificationRecipientJpaEntity> savedOpt = saveHelper.saveAndFlushRequiresNew(toJpaEntity(item));
        if (savedOpt.isPresent()) {
            return toDomain(savedOpt.get());
        }
        // Concurrent race: record was already created by another thread/transaction.
        // Fetch the existing record to guarantee idempotent return.
        return repository.findByNotificationEventIdAndRecipientUserId(
                item.getEventId().value(),
                item.getRecipientUserId().value()
        ).map(this::toDomain).orElse(item);
    }

    @Override
    public Optional<NotificationRecipientItem> findById(NotificationRecipientId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<NotificationRecipientItem> findByEventIdAndRecipientUserId(NotificationEventId eventId, UserId recipientUserId) {
        if (eventId == null || eventId.value() == null || recipientUserId == null || recipientUserId.value() == null) {
            return Optional.empty();
        }
        return repository.findByNotificationEventIdAndRecipientUserId(eventId.value(), recipientUserId.value())
                .map(this::toDomain);
    }

    @Override
    public List<NotificationRecipientItem> findRecipients(UserId recipientUserId, String status, String level, int page, int size) {
        if (recipientUserId == null || recipientUserId.value() == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Boolean readFilter = parseReadFilter(status);
        String levelFilter = parseLevelFilter(level);

        return repository.findByFilters(recipientUserId.value(), readFilter, levelFilter, pageable)
                .getContent()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countRecipients(UserId recipientUserId, String status, String level) {
        if (recipientUserId == null || recipientUserId.value() == null) {
            return 0L;
        }
        Boolean readFilter = parseReadFilter(status);
        String levelFilter = parseLevelFilter(level);
        return repository.countByFilters(recipientUserId.value(), readFilter, levelFilter);
    }

    @Override
    public long countUnread(UserId recipientUserId) {
        if (recipientUserId == null || recipientUserId.value() == null) {
            return 0L;
        }
        return repository.countByRecipientUserIdAndIsDeletedFalseAndIsReadFalse(recipientUserId.value());
    }

    @Override
    public int markAllAsRead(UserId recipientUserId, LocalDateTime now) {
        if (recipientUserId == null || recipientUserId.value() == null) {
            return 0;
        }
        return repository.markAllAsRead(recipientUserId.value(), now != null ? now : LocalDateTime.now());
    }

    @Override
    public long purgeRecipientsOlderThan(LocalDateTime cutoff) {
        if (cutoff == null) {
            return 0L;
        }
        return repository.deleteRecipientsByEventCreatedAtBefore(cutoff);
    }

    private Boolean parseReadFilter(String status) {
        if (status == null || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        if (status.equalsIgnoreCase("UNREAD")) {
            return false;
        }
        if (status.equalsIgnoreCase("READ")) {
            return true;
        }
        return null;
    }

    private String parseLevelFilter(String level) {
        if (level == null || level.equalsIgnoreCase("ALL")) {
            return null;
        }
        return level.trim().toUpperCase();
    }

    private NotificationRecipientJpaEntity toJpaEntity(NotificationRecipientItem domain) {
        return new NotificationRecipientJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getEventId().value(),
                domain.getRecipientUserId().value(),
                domain.isRead(),
                domain.getReadAt(),
                domain.isDeleted(),
                domain.getDeletedAt(),
                domain.getCreatedAt()
        );
    }

    private NotificationRecipientItem toDomain(NotificationRecipientJpaEntity entity) {
        return new NotificationRecipientItem(
                NotificationRecipientId.of(entity.getId()),
                NotificationEventId.of(entity.getNotificationEventId()),
                new UserId(entity.getRecipientUserId()),
                entity.isRead(),
                entity.getReadAt(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                entity.getCreatedAt()
        );
    }
}
