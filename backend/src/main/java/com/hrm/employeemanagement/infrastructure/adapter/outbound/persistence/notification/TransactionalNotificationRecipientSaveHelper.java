package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationRecipientJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRecipientRepository;

/**
 * Helper thực hiện lưu NotificationRecipientJpaEntity trong transaction REQUIRES_NEW.
 * Ngăn chặn lỗi rollback-only của Hibernate khi có xung đột unique constraint uk_notification_recipient_event_user.
 */
@Component
public class TransactionalNotificationRecipientSaveHelper {

    private static final Logger log = LoggerFactory.getLogger(TransactionalNotificationRecipientSaveHelper.class);

    private final SpringDataNotificationRecipientRepository repository;

    public TransactionalNotificationRecipientSaveHelper(SpringDataNotificationRecipientRepository repository) {
        this.repository = repository;
    }

    /**
     * Chèn bản ghi recipient mới trong sub-transaction độc lập (REQUIRES_NEW).
     * Nếu xảy ra xung đột unique constraint (uk_notification_recipient_event_user) do concurrent requests,
     * sub-transaction rollback độc lập và trả về Optional.empty().
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationRecipientJpaEntity> saveAndFlushRequiresNew(NotificationRecipientJpaEntity entity) {
        try {
            return Optional.of(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Xung đột đồng thời khi tạo recipient (eventId={}, userId={}), bỏ qua để đảm bảo idempotent.",
                    entity.getNotificationEventId(), entity.getRecipientUserId());
            return Optional.empty();
        }
    }
}
