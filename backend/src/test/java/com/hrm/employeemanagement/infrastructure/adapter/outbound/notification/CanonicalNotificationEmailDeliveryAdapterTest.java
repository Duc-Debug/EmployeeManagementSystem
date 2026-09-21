package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryDecision;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.TransactionalEmailDigestHelper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

class CanonicalNotificationEmailDeliveryAdapterTest {
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Test
    void immediateEmailUsesCanonicalSourceKeyForIdempotency() {
        Fixture fixture = fixture();
        NotificationEvent event = event("SOURCE:1");
        when(fixture.outbox.findByRecipientUserIdAndSourceEventKey(1L, "SOURCE:1"))
                .thenReturn(Optional.empty());

        fixture.adapter.schedule(event, new UserId(1L),
                new NotificationDeliveryDecision(true, LocalDateTime.now(clock), NotificationFrequency.IMMEDIATE));

        ArgumentCaptor<NotificationEmailOutboxJpaEntity> captor =
                ArgumentCaptor.forClass(NotificationEmailOutboxJpaEntity.class);
        verify(fixture.digestHelper).create(captor.capture());
        assertEquals("SOURCE:1", captor.getValue().getSourceEventKey());
    }

    @Test
    void digestEmailClaimsSourceItemBeforeAppending() {
        Fixture fixture = fixture();
        NotificationEvent event = event("SOURCE:2");
        LocalDateTime availableAt = LocalDateTime.of(2026, 9, 21, 17, 0);
        NotificationEmailOutboxJpaEntity batch = mock(NotificationEmailOutboxJpaEntity.class);
        when(batch.getId()).thenReturn(99L);
        when(fixture.outbox.findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                1L, availableAt, NotificationFrequency.DAILY_DIGEST.name()))
                .thenReturn(Optional.of(batch));

        fixture.adapter.schedule(event, new UserId(1L),
                new NotificationDeliveryDecision(true, availableAt, NotificationFrequency.DAILY_DIGEST));

        verify(fixture.digestHelper).appendIfAbsent(
                eq(99L), eq("SOURCE:2"), any(), eq(LocalDateTime.now(clock)));
    }

    private Fixture fixture() {
        LoadUserPort users = mock(LoadUserPort.class);
        SpringDataNotificationEmailOutboxRepository outbox = mock(SpringDataNotificationEmailOutboxRepository.class);
        TransactionalEmailDigestHelper digestHelper = mock(TransactionalEmailDigestHelper.class);
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getUsername()).thenReturn("user");
        when(users.findById(new UserId(1L))).thenReturn(Optional.of(user));
        return new Fixture(new CanonicalNotificationEmailDeliveryAdapter(users, outbox, digestHelper, clock),
                outbox, digestHelper);
    }

    private NotificationEvent event(String sourceKey) {
        return new NotificationEvent(new NotificationEventId(10L), "TASK_DUE_REMINDER",
                NotificationLevel.THAP, "Title", "Message", "TASK", "10", sourceKey,
                LocalDateTime.now(clock));
    }

    private record Fixture(
            CanonicalNotificationEmailDeliveryAdapter adapter,
            SpringDataNotificationEmailOutboxRepository outbox,
            TransactionalEmailDigestHelper digestHelper) {}
}
