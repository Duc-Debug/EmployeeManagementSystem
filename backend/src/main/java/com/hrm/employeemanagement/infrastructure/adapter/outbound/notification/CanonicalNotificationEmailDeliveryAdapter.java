package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEmailDeliveryPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryDecision;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.TransactionalEmailDigestHelper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

@Component
public class CanonicalNotificationEmailDeliveryAdapter implements NotificationEmailDeliveryPort {
    private final LoadUserPort loadUserPort;
    private final SpringDataNotificationEmailOutboxRepository outboxRepository;
    private final TransactionalEmailDigestHelper digestHelper;
    private final Clock clock;

    public CanonicalNotificationEmailDeliveryAdapter(
            LoadUserPort loadUserPort,
            SpringDataNotificationEmailOutboxRepository outboxRepository,
            TransactionalEmailDigestHelper digestHelper,
            Clock clock) {
        this.loadUserPort = Objects.requireNonNull(loadUserPort);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.digestHelper = Objects.requireNonNull(digestHelper);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void schedule(
            NotificationEvent sourceEvent, UserId recipientUserId, NotificationDeliveryDecision decision) {
        loadUserPort.findById(recipientUserId)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .ifPresent(user -> scheduleForAddress(
                        sourceEvent, recipientUserId, user.getEmail(), user.getUsername(), decision));
    }

    private void scheduleForAddress(
            NotificationEvent event, UserId recipient, String email, String name,
            NotificationDeliveryDecision decision) {
        if (!decision.isDigest()) {
            scheduleImmediate(event, recipient, email, name, decision);
            return;
        }
        String frequency = decision.frequency().name();
        String item = formatItem(event);
        var existing = outboxRepository
                .findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                        recipient.value(), decision.availableAt(), frequency);
        if (existing.isPresent()) {
            appendIfAbsent(existing.get().getId(), event, item);
            return;
        }
        NotificationEmailOutboxJpaEntity batch = new NotificationEmailOutboxJpaEntity(
                recipient.value(), email, name,
                decision.frequency() == NotificationFrequency.DAILY_DIGEST
                        ? "Bản tin thông báo hàng ngày" : "Bản tin thông báo hàng tuần",
                "", decision.availableAt(), LocalDateTime.now(clock), frequency);
        try {
            NotificationEmailOutboxJpaEntity created = digestHelper.create(batch);
            appendIfAbsent(created.getId(), event, item);
        } catch (DataIntegrityViolationException concurrentInsert) {
            var winner = outboxRepository
                    .findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                            recipient.value(), decision.availableAt(), frequency)
                    .orElseThrow(() -> concurrentInsert);
            appendIfAbsent(winner.getId(), event, item);
        }
    }

    private void scheduleImmediate(
            NotificationEvent event, UserId recipient, String email, String name,
            NotificationDeliveryDecision decision) {
        if (outboxRepository.findByRecipientUserIdAndSourceEventKey(
                recipient.value(), event.getSourceEventKey()).isPresent()) {
            return;
        }
        NotificationEmailOutboxJpaEntity message = new NotificationEmailOutboxJpaEntity(
                recipient.value(), email, name, event.getTitle(), event.getMessage(),
                decision.availableAt(), LocalDateTime.now(clock), null);
        message.setSourceEventKey(event.getSourceEventKey());
        try {
            digestHelper.create(message);
        } catch (DataIntegrityViolationException duplicate) {
            // The unique recipient/source key makes retries and concurrent delivery idempotent.
        }
    }

    private void appendIfAbsent(Long outboxId, NotificationEvent event, String item) {
        try {
            digestHelper.appendIfAbsent(
                    outboxId, event.getSourceEventKey(), item, LocalDateTime.now(clock));
        } catch (DataIntegrityViolationException duplicateItem) {
            // Already aggregated into this channel batch.
        }
    }

    private String formatItem(NotificationEvent event) {
        return "• " + event.getTitle()
                + (event.getMessage() == null || event.getMessage().isBlank()
                        ? "" : ": " + event.getMessage());
    }
}
