package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfigHistory;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupConfigHistoryJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDedupConfigHistoryRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDedupConfigRepository;

@Component
public class NotificationDedupConfigPersistenceAdapter implements NotificationDedupConfigRepositoryPort {

    private final SpringDataNotificationDedupConfigRepository configRepository;
    private final SpringDataNotificationDedupConfigHistoryRepository historyRepository;

    public NotificationDedupConfigPersistenceAdapter(
            SpringDataNotificationDedupConfigRepository configRepository,
            SpringDataNotificationDedupConfigHistoryRepository historyRepository
    ) {
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository must not be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository must not be null");
    }

    @Override
    public NotificationDedupConfig loadConfig() {
        return configRepository.findById(1L)
                .map(this::toDomain)
                .orElseGet(NotificationDedupConfig::defaultConfig);
    }

    @Override
    public NotificationDedupConfig saveConfig(NotificationDedupConfig config) {
        NotificationDedupConfigJpaEntity entity = configRepository.findById(1L)
                .orElse(new NotificationDedupConfigJpaEntity());

        entity.setId(1L);
        entity.setEnabled(config.isEnabled());
        entity.setDedupWindowDays(config.getDedupWindowDays());
        entity.setScanIntervalMinutes(config.getScanIntervalMinutes());
        entity.setUpdatedBy(config.getUpdatedBy());
        entity.setUpdatedAt(config.getUpdatedAt());
        if (config.getVersion() != null) {
            entity.setVersion(config.getVersion());
        }

        NotificationDedupConfigJpaEntity saved = configRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void saveHistory(NotificationDedupConfigHistory history) {
        NotificationDedupConfigHistoryJpaEntity entity = new NotificationDedupConfigHistoryJpaEntity(
                null,
                history.getActorUserId(),
                history.getAction(),
                history.getPreviousValue(),
                history.getNewValue(),
                history.getChangeSummary(),
                history.getCreatedAt()
        );
        historyRepository.save(entity);
    }

    private NotificationDedupConfig toDomain(NotificationDedupConfigJpaEntity entity) {
        return new NotificationDedupConfig(
                entity.getId(),
                entity.isEnabled(),
                entity.getDedupWindowDays(),
                entity.getScanIntervalMinutes(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
