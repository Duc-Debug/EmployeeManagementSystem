package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationAuditLogRepositoryPort;
import com.hrm.employeemanagement.domain.notification.NotificationAuditAction;
import com.hrm.employeemanagement.domain.notification.NotificationAuditLog;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationAuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationAuditLogRepository;

@Component
public class NotificationAuditLogPersistenceAdapter implements NotificationAuditLogRepositoryPort {

    private final SpringDataNotificationAuditLogRepository repository;

    public NotificationAuditLogPersistenceAdapter(SpringDataNotificationAuditLogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public NotificationAuditLog save(NotificationAuditLog auditLog) {
        NotificationAuditLogJpaEntity entity = toJpaEntity(auditLog);
        NotificationAuditLogJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<NotificationAuditLog> findByActorUserId(UserId actorUserId) {
        if (actorUserId == null || actorUserId.value() == null) {
            return List.of();
        }
        return repository.findByActorUserIdOrderByCreatedAtDesc(actorUserId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private NotificationAuditLogJpaEntity toJpaEntity(NotificationAuditLog domain) {
        return new NotificationAuditLogJpaEntity(
                domain.getId(),
                domain.getActorUserId().value(),
                domain.getAction().name(),
                domain.getTargetType(),
                domain.getTargetId(),
                domain.getDetail(),
                domain.getCreatedAt()
        );
    }

    private NotificationAuditLog toDomain(NotificationAuditLogJpaEntity entity) {
        return new NotificationAuditLog(
                entity.getId(),
                new UserId(entity.getActorUserId()),
                NotificationAuditAction.valueOf(entity.getAction()),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getDetail(),
                entity.getCreatedAt()
        );
    }
}
