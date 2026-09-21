package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

@Component
public class TransactionalEmailDigestHelper {
    private final SpringDataNotificationEmailOutboxRepository repository;

    public TransactionalEmailDigestHelper(SpringDataNotificationEmailOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationEmailOutboxJpaEntity create(NotificationEmailOutboxJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(Long id, String item) {
        repository.appendDigestItem(id, item);
    }
}
