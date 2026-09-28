package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupRepositoryPort;
import com.hrm.employeemanagement.domain.notification.dedup.DedupRecordStatus;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupRecord;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupRecordJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDedupRecordRepository;

@Component
public class NotificationDedupPersistenceAdapter implements NotificationDedupRepositoryPort {

    private final SpringDataNotificationDedupRecordRepository recordRepository;

    public NotificationDedupPersistenceAdapter(SpringDataNotificationDedupRecordRepository recordRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository must not be null");
    }

    @Override
    public Optional<NotificationDedupRecord> findActiveByDedupKey(String dedupKey) {
        return recordRepository.findByActiveDedupKey(dedupKey)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public NotificationDedupRecord save(NotificationDedupRecord record) {
        NotificationDedupRecordJpaEntity entity;
        if (record.getId() != null) {
            entity = recordRepository.findById(record.getId())
                    .orElse(new NotificationDedupRecordJpaEntity());
        } else {
            entity = new NotificationDedupRecordJpaEntity();
        }

        entity.setDedupKey(record.getDedupKey());
        entity.setActiveDedupKey(record.getActiveDedupKey());
        entity.setEventType(record.getEventType());
        entity.setTargetEntityType(record.getTargetEntityType());
        entity.setTargetEntityId(record.getTargetEntityId());
        entity.setYearWeek(record.getYearWeek());
        entity.setRecipientUserId(record.getRecipientUserId());
        entity.setStatus(record.getStatus().name());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setResolvedAt(record.getResolvedAt());
        entity.setExpiresAt(record.getExpiresAt());

        NotificationDedupRecordJpaEntity saved = recordRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<NotificationDedupRecord> findActiveByTargetAndWeek(String targetEntityType, String targetEntityId, String yearWeek) {
        return recordRepository.findByTargetEntityTypeAndTargetEntityIdAndYearWeekAndStatus(
                targetEntityType,
                targetEntityId,
                yearWeek,
                DedupRecordStatus.ACTIVE.name()
        ).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void resolveActiveRecords(String targetEntityType, String targetEntityId, String yearWeek, LocalDateTime resolvedAt) {
        recordRepository.resolveActiveRecords(targetEntityType, targetEntityId, yearWeek, resolvedAt != null ? resolvedAt : LocalDateTime.now());
    }

    private NotificationDedupRecord toDomain(NotificationDedupRecordJpaEntity entity) {
        return new NotificationDedupRecord(
                entity.getId(),
                entity.getDedupKey(),
                entity.getActiveDedupKey(),
                entity.getEventType(),
                entity.getTargetEntityType(),
                entity.getTargetEntityId(),
                entity.getYearWeek(),
                entity.getRecipientUserId(),
                DedupRecordStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getExpiresAt()
        );
    }
}
