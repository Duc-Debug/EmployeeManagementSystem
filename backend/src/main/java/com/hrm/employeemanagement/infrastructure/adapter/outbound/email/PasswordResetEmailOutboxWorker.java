package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.PasswordResetEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.SpringDataPasswordResetEmailOutboxRepository;

@Component
public class PasswordResetEmailOutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailOutboxWorker.class);
    private static final long CLAIM_LEASE_MINUTES = 5;

    private final SpringDataPasswordResetEmailOutboxRepository repository;
    private final EmailSenderPort emailPort;
    private final TransactionOperations transactionOperations;

    @Autowired
    public PasswordResetEmailOutboxWorker(
            SpringDataPasswordResetEmailOutboxRepository repository,
            EmailSenderPort emailPort,
            PlatformTransactionManager transactionManager) {
        this(repository, emailPort, new TransactionTemplate(transactionManager));
    }

    public PasswordResetEmailOutboxWorker(
            SpringDataPasswordResetEmailOutboxRepository repository,
            EmailSenderPort emailPort,
            TransactionOperations transactionOperations) {
        this.repository = repository;
        this.emailPort = emailPort;
        this.transactionOperations = transactionOperations;
    }

    public PasswordResetEmailOutboxWorker(
            SpringDataPasswordResetEmailOutboxRepository repository,
            EmailSenderPort emailPort) {
        this(repository, emailPort, TransactionOperations.withoutTransaction());
    }

    /**
     * Polls and delivers the next pending email outbox message.
     * Transaction boundary is split into:
     * 1. Short transaction: Lock and claim message (advance availableAt lease).
     * 2. Outside transaction: Send email via network to avoid holding database connection.
     * 3. Short transaction: Mark message delivered or schedule retry.
     */
    @Scheduled(fixedDelayString = "${app.email-outbox.poll-delay-ms:5000}")
    public void deliverNext() {
        // Bước 1: Lấy và đánh dấu khóa bản ghi đang được xử lý trong transaction ngắn
        ClaimedOutboxMessage claimed = transactionOperations.execute(status -> claimNext());
        if (claimed == null) {
            return;
        }

        // Bước 2: Gửi email bên ngoài transaction để không giữ kết nối DB
        boolean success = false;
        String errorMessage = null;
        try {
            emailPort.sendPasswordResetEmail(
                    claimed.recipientEmail(),
                    claimed.username(),
                    claimed.resetToken(),
                    claimed.validityMinutes()
            );
            success = true;
        } catch (RuntimeException ex) {
            errorMessage = ex.getMessage();
            log.warn("Password reset email delivery failed; queued for retry: {}", claimed.recipientEmail(), ex);
        }

        // Bước 3: Mở transaction mới để cập nhật trạng thái gửi thành công hoặc lên lịch gửi lại
        final boolean isDelivered = success;
        final String error = errorMessage;
        transactionOperations.executeWithoutResult(status -> finalizeDelivery(claimed, isDelivered, error));
    }

    private ClaimedOutboxMessage claimNext() {
        Instant now = Instant.now();
        Optional<PasswordResetEmailOutboxJpaEntity> optionalRecord =
                repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(now);

        if (optionalRecord.isEmpty()) {
            return null;
        }

        PasswordResetEmailOutboxJpaEntity record = optionalRecord.get();
        if (record.isExpired(now)) {
            record.markExpired(now);
            repository.save(record);
            log.warn("Password reset email token expired before delivery for recipient: {}", record.getRecipientEmail());
            return null;
        }

        // Đánh dấu lease xử lý để các worker khác không gửi trùng
        record.claimForProcessing(now.plus(CLAIM_LEASE_MINUTES, ChronoUnit.MINUTES));
        repository.save(record);

        return new ClaimedOutboxMessage(
                record.getId(),
                record.getRecipientEmail(),
                record.getUsername(),
                record.getResetToken(),
                record.getValidityMinutes(),
                record
        );
    }

    private void finalizeDelivery(ClaimedOutboxMessage claimed, boolean success, String errorMessage) {
        Instant now = Instant.now();
        PasswordResetEmailOutboxJpaEntity record = claimed.id() != null
                ? repository.findById(claimed.id()).orElse(claimed.entity())
                : claimed.entity();

        if (success) {
            record.markDelivered(now);
        } else {
            record.scheduleRetry(now.plus(1, ChronoUnit.MINUTES), errorMessage);
        }
        repository.save(record);
    }

    public record ClaimedOutboxMessage(
            Long id,
            String recipientEmail,
            String username,
            String resetToken,
            long validityMinutes,
            PasswordResetEmailOutboxJpaEntity entity
    ) {}
}
