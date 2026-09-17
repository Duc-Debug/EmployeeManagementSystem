package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEventJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEventRepository;

@Component
public class NotificationEventPersistenceAdapter implements NotificationEventRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPersistenceAdapter.class);

    private final SpringDataNotificationEventRepository repository;
    private final TransactionalNotificationEventSaveHelper txHelper;

    public NotificationEventPersistenceAdapter(
            SpringDataNotificationEventRepository repository,
            TransactionalNotificationEventSaveHelper txHelper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.txHelper = Objects.requireNonNull(txHelper, "txHelper must not be null");
    }

    @Override
    public NotificationEvent save(NotificationEvent event) {
        NotificationEventJpaEntity entity = toJpaEntity(event);
        NotificationEventJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public NotificationEvent getOrCreate(NotificationEvent event) {
        // 1. Kiểm tra nếu đã tồn tại
        Optional<NotificationEventJpaEntity> existing = repository.findBySourceEventKey(event.getSourceEventKey());
        if (existing.isPresent()) {
            return toDomain(existing.get());
        }

        // 2. Thử lưu trong sub-transaction REQUIRES_NEW để cô lập lỗi constraint
        NotificationEventJpaEntity newEntity = toJpaEntity(event);
        try {
            NotificationEventJpaEntity saved = txHelper.saveAndFlushRequiresNew(newEntity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            log.info("Xung đột đồng thời (race condition) với source_event_key: {}. Tự động tải lại bản ghi đã tạo.",
                    event.getSourceEventKey());
            // 3. Nếu bị vi phạm UNIQUE do thread khác đã chèn trước, re-fetch bản ghi hiện có
            return repository.findBySourceEventKey(event.getSourceEventKey())
                    .map(this::toDomain)
                    .orElseThrow(() -> new IllegalStateException("Không thể tạo hoặc tìm thấy notification event với key: " + event.getSourceEventKey(), e));
        }
    }

    @Override
    public Optional<NotificationEvent> findById(NotificationEventId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<NotificationEvent> findBySourceEventKey(String sourceEventKey) {
        if (sourceEventKey == null || sourceEventKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findBySourceEventKey(sourceEventKey.trim()).map(this::toDomain);
    }

    @Override
    public long purgeOrphanEventsOlderThan(LocalDateTime cutoff) {
        if (cutoff == null) {
            return 0L;
        }
        return repository.deleteOrphanEventsOlderThan(cutoff);
    }

    private NotificationEventJpaEntity toJpaEntity(NotificationEvent domain) {
        return new NotificationEventJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getEventType(),
                domain.getLevel().name(),
                domain.getTitle(),
                domain.getMessage(),
                domain.getRelatedEntityType(),
                domain.getRelatedEntityId(),
                domain.getSourceEventKey(),
                domain.getCreatedAt()
        );
    }

    private NotificationEvent toDomain(NotificationEventJpaEntity entity) {
        return new NotificationEvent(
                NotificationEventId.of(entity.getId()),
                entity.getEventType(),
                NotificationLevel.fromString(entity.getLevel()),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRelatedEntityType(),
                entity.getRelatedEntityId(),
                entity.getSourceEventKey(),
                entity.getCreatedAt()
        );
    }
}
