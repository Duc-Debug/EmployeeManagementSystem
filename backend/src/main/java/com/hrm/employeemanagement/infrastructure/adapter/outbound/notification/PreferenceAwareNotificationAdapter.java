package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryTimePolicy;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.NotificationRepositoryAdapter;

/** Applies the recipient's in-app preference before a legacy notification is persisted. */
@Component
@Primary
public class PreferenceAwareNotificationAdapter implements SaveNotificationPort {

    private final NotificationRepositoryAdapter delegate;
    private final LoadNotificationPreferencePort preferencePort;
    private final Clock clock;

    public PreferenceAwareNotificationAdapter(
            NotificationRepositoryAdapter delegate,
            LoadNotificationPreferencePort preferencePort,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.preferencePort = Objects.requireNonNull(preferencePort, "preferencePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Notification save(Notification notification) {
        if (notification == null || !isInAppEnabled(notification)) {
            return notification;
        }
        return delegate.save(schedule(notification));
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        delegate.saveAll(notifications.stream().filter(this::isInAppEnabled).map(this::schedule).toList());
    }

    @Override
    public void markAllAsRead(UserId recipientId) {
        delegate.markAllAsRead(recipientId);
    }

    private boolean isInAppEnabled(Notification notification) {
        NotificationPreference preference = preferencePort.findByUserId(notification.getRecipientId())
                .orElseGet(() -> NotificationPreference.createDefault(notification.getRecipientId()));
        return preference.isChannelActiveFor(
                notification.getType(),
                false,
                LocalTime.now(clock)
        );
    }

    private Notification schedule(Notification notification) {
        NotificationPreference preference = preferencePort.findByUserId(notification.getRecipientId())
                .orElseGet(() -> NotificationPreference.createDefault(notification.getRecipientId()));
        java.time.LocalDateTime releaseAt = NotificationDeliveryTimePolicy.releaseAt(
                preference.getFrequency(), notification.getType(), java.time.LocalDateTime.now(clock));
        return new Notification(
                notification.getId(), notification.getRecipientId(), notification.getSenderId(),
                notification.getType(), notification.getTargetType(), notification.getTargetId(),
                notification.getTitle(), notification.getContent(), notification.isRead(), releaseAt);
    }
}
