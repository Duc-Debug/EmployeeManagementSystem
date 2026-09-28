package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEventJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEventRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDigestItemRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDigestItemJpaEntity;
import java.time.LocalDateTime;

@Component
public class TransactionalNotificationEventSaveHelper {

    private final SpringDataNotificationEventRepository repository;
    private final SpringDataNotificationDigestItemRepository digestItemRepository;

    public TransactionalNotificationEventSaveHelper(
            SpringDataNotificationEventRepository repository,
            SpringDataNotificationDigestItemRepository digestItemRepository) {
        this.repository = repository;
        this.digestItemRepository = digestItemRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationEventJpaEntity saveAndFlushRequiresNew(NotificationEventJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendDigestItemRequiresNew(
            Long eventId, String sourceEventKey, String itemText, LocalDateTime createdAt) {
        digestItemRepository.saveAndFlush(
                new NotificationDigestItemJpaEntity(eventId, sourceEventKey, itemText, createdAt));
        repository.appendDigestItem(eventId, itemText);
    }
}
