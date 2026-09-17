package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEventJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEventRepository;

@Component
public class TransactionalNotificationEventSaveHelper {

    private final SpringDataNotificationEventRepository repository;

    public TransactionalNotificationEventSaveHelper(SpringDataNotificationEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationEventJpaEntity saveAndFlushRequiresNew(NotificationEventJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }
}
