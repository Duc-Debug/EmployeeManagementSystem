package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRepository;

@Component
public class TransactionalLegacyDigestHelper {
    private final SpringDataNotificationRepository repository;

    public TransactionalLegacyDigestHelper(SpringDataNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationJpaEntity create(NotificationJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(Long digestId, String item) {
        repository.appendDigestItem(digestId, item);
    }
}
