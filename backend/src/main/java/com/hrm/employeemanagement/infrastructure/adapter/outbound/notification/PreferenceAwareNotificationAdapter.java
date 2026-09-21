package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryTimePolicy;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryDecision;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.NotificationRepositoryAdapter;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.TransactionalEmailDigestHelper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

/** Applies the recipient's in-app preference before a legacy notification is persisted. */
@Component
@Primary
public class PreferenceAwareNotificationAdapter implements SaveNotificationPort {

    private final NotificationRepositoryAdapter delegate;
    private final LoadNotificationPreferencePort preferencePort;
    private final Clock clock;
    private final LoadUserPort loadUserPort;
    private final SpringDataNotificationEmailOutboxRepository emailOutboxRepository;
    private final TransactionalEmailDigestHelper emailDigestHelper;

    public PreferenceAwareNotificationAdapter(
            NotificationRepositoryAdapter delegate,
            LoadNotificationPreferencePort preferencePort,
            Clock clock,
            LoadUserPort loadUserPort,
            SpringDataNotificationEmailOutboxRepository emailOutboxRepository,
            TransactionalEmailDigestHelper emailDigestHelper
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.preferencePort = Objects.requireNonNull(preferencePort, "preferencePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.emailOutboxRepository = Objects.requireNonNull(emailOutboxRepository, "emailOutboxRepository must not be null");
        this.emailDigestHelper = Objects.requireNonNull(emailDigestHelper, "emailDigestHelper must not be null");
    }

    @Override
    public Notification save(Notification notification) {
        if (notification == null) {
            return notification;
        }
        NotificationPreference preference = loadPreference(notification);
        if (notification.getId() == null) {
            queueEmailIfEnabled(notification, preference);
        }
        PreparedNotification prepared = prepare(notification, preference);
        if (prepared == null) {
            return notification;
        }
        if (prepared.decision().isDigest()) {
            return delegate.appendToDigest(notification, prepared.decision().availableAt(),
                    prepared.decision().frequency(), clock.getZone());
        }
        return delegate.save(prepared.notification());
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        notifications.forEach(this::save);
    }

    @Override
    public void markAllAsRead(UserId recipientId) {
        delegate.markAllAsRead(recipientId);
    }

    private PreparedNotification prepare(Notification notification, NotificationPreference preference) {
        NotificationDeliveryDecision decision = NotificationDeliveryTimePolicy.decide(
                preference, notification.getType(), false, LocalDateTime.now(clock));
        return decision.enabled()
                ? new PreparedNotification(notification.scheduledFor(decision.availableAt()), decision)
                : null;
    }

    private NotificationPreference loadPreference(Notification notification) {
        return preferencePort.findByUserId(notification.getRecipientId())
                .orElseGet(() -> NotificationPreference.createDefault(notification.getRecipientId()));
    }

    private void queueEmailIfEnabled(Notification notification, NotificationPreference preference) {
        NotificationDeliveryDecision decision = NotificationDeliveryTimePolicy.decide(
                preference, notification.getType(), true, LocalDateTime.now(clock));
        if (!decision.enabled()) {
            return;
        }
        loadUserPort.findById(notification.getRecipientId())
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .ifPresent(user -> enqueueEmail(notification, user.getEmail(), user.getUsername(), decision));
    }

    private void enqueueEmail(Notification notification, String email, String name,
            NotificationDeliveryDecision decision) {
        String digestFrequency = decision.isDigest() ? decision.frequency().name() : null;
        String item = "• " + notification.getTitle()
                + (notification.getContent() == null || notification.getContent().isBlank()
                        ? "" : ": " + notification.getContent());
        if (digestFrequency != null) {
            var existing = emailOutboxRepository
                    .findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                            notification.getRecipientId().value(), decision.availableAt(), digestFrequency);
            if (existing.isPresent()) {
                emailDigestHelper.append(existing.get().getId(), item);
                return;
            }
        }
        String subject = digestFrequency == null ? notification.getTitle()
                : decision.frequency() == com.hrm.employeemanagement.domain.notification.NotificationFrequency.DAILY_DIGEST
                        ? "Bản tin thông báo hàng ngày" : "Bản tin thông báo hàng tuần";
        String body = digestFrequency == null ? notification.getContent() : item;
        NotificationEmailOutboxJpaEntity newMessage = new NotificationEmailOutboxJpaEntity(
                notification.getRecipientId().value(), email, name, subject,
                body == null ? "" : body, decision.availableAt(), LocalDateTime.now(clock), digestFrequency);
        if (digestFrequency == null) {
            String sourceEventKey = notification.getSourceEventKey();
            if (emailOutboxRepository.findByRecipientUserIdAndSourceEventKey(
                    notification.getRecipientId().value(), sourceEventKey).isPresent()) {
                return;
            }
            newMessage.setSourceEventKey(sourceEventKey);
            try {
                emailDigestHelper.create(newMessage);
            } catch (DataIntegrityViolationException duplicate) {
                // A concurrent retry already created this recipient/source email.
            }
            return;
        }
        try {
            emailDigestHelper.create(newMessage);
        } catch (DataIntegrityViolationException concurrentInsert) {
            var winner = emailOutboxRepository
                    .findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                            notification.getRecipientId().value(), decision.availableAt(), digestFrequency)
                    .orElseThrow(() -> concurrentInsert);
            emailDigestHelper.append(winner.getId(), item);
        }
    }

    private record PreparedNotification(Notification notification, NotificationDeliveryDecision decision) {}
}
