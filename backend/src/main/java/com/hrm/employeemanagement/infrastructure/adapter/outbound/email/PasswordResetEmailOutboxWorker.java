package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.PasswordResetEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.SpringDataPasswordResetEmailOutboxRepository;

@Component
public class PasswordResetEmailOutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailOutboxWorker.class);

    private final SpringDataPasswordResetEmailOutboxRepository repository;
    private final SimulatedEmailPort emailPort;

    public PasswordResetEmailOutboxWorker(SpringDataPasswordResetEmailOutboxRepository repository,
                                          SimulatedEmailPort emailPort) {
        this.repository = repository;
        this.emailPort = emailPort;
    }

    @Scheduled(fixedDelayString = "${app.email-outbox.poll-delay-ms:5000}")
    @Transactional
    public void deliverNext() {
        repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(Instant.now())
                .ifPresent(this::deliver);
    }

    private void deliver(PasswordResetEmailOutboxJpaEntity message) {
        try {
            emailPort.sendPasswordResetEmail(message.getRecipientEmail(), message.getUsername(),
                    message.getResetToken(), message.getValidityMinutes());
            message.markDelivered(Instant.now());
        } catch (RuntimeException ex) {
            message.scheduleRetry(Instant.now().plus(1, ChronoUnit.MINUTES), ex.getMessage());
            log.warn("Password reset email delivery failed; queued for retry: {}", message.getRecipientEmail());
        }
    }
}
