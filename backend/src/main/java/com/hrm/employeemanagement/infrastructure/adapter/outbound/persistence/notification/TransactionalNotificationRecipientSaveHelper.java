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
            if (isRecipientUniqueConstraintViolation(ex)) {
                log.debug("Xung đột unique constraint khi tạo recipient (eventId={}, userId={}), bỏ qua để xử lý idempotent.",
                        entity.getNotificationEventId(), entity.getRecipientUserId());
                return Optional.empty();
            }
            // Không phải lỗi duplicate key của recipient (ví dụ: Foreign Key violation, NULL violation) -> rethrow!
            log.error("Lỗi ràng buộc toàn vẹn không phải duplicate recipient: {}", ex.getMessage());
            throw ex;
        }
    }

    private boolean isRecipientUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        if (cause == null) {
            cause = ex.getCause();
        }
        String message = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";

        // 1. Kiểm tra chính xác tên unique constraint uk_notification_recipient_event_user
        if (message.contains("uk_notification_recipient_event_user")) {
            return true;
        }

        // 2. Kiểm tra vendor duplicate key code (MySQL 1062, ANSI SQLState 23000 / 23505)
        if (cause instanceof java.sql.SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();
            String sqlState = sqlEx.getSQLState();
            if (errorCode == 1062 || "23000".equals(sqlState) || "23505".equals(sqlState)) {
                return message.contains("notification_recipients")
                        || message.contains("uk_notification_recipient_event_user")
                        || message.contains("duplicate")
                        || message.contains("trùng");
            }
        }

        return message.contains("duplicate entry") && (message.contains("key") || message.contains("unique"));
    }
}