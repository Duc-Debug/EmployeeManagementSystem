package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailDigestItemRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailDigestItemJpaEntity;
import java.time.LocalDateTime;

@Component
public class TransactionalEmailDigestHelper {
    private final SpringDataNotificationEmailOutboxRepository repository;
    private final SpringDataNotificationEmailDigestItemRepository itemRepository;

    public TransactionalEmailDigestHelper(
            SpringDataNotificationEmailOutboxRepository repository,
            SpringDataNotificationEmailDigestItemRepository itemRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationEmailOutboxJpaEntity create(NotificationEmailOutboxJpaEntity entity) {
        return repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(Long id, String item) {
        repository.appendDigestItem(id, item);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendIfAbsent(Long id, String sourceEventKey, String item, LocalDateTime createdAt) {
        itemRepository.saveAndFlush(
                new NotificationEmailDigestItemJpaEntity(id, sourceEventKey, createdAt));
        repository.appendDigestItem(id, item);
    }
}
