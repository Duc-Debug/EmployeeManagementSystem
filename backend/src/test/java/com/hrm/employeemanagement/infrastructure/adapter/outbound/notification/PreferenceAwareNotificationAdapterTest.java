package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.NotificationRepositoryAdapter;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.TransactionalEmailDigestHelper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationEmailOutboxRepository;

class PreferenceAwareNotificationAdapterTest {

    @Test
    void immediateEmailCarriesSourceKeyAndDuplicateIsNotCreated() {
        NotificationRepositoryAdapter delegate = mock(NotificationRepositoryAdapter.class);
        LoadNotificationPreferencePort preferencePort = mock(LoadNotificationPreferencePort.class);
        LoadUserPort userPort = mock(LoadUserPort.class);
        SpringDataNotificationEmailOutboxRepository outbox = mock(SpringDataNotificationEmailOutboxRepository.class);
        TransactionalEmailDigestHelper emailDigestHelper = mock(TransactionalEmailDigestHelper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        UserId userId = new UserId(1L);
        NotificationPreference preference = NotificationPreference.createDefault(userId);
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getUsername()).thenReturn("user");
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(userPort.findById(userId)).thenReturn(Optional.of(user));
        when(outbox.findByRecipientUserIdAndSourceEventKey(eq(1L), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mock(NotificationEmailOutboxJpaEntity.class)));

        PreferenceAwareNotificationAdapter adapter = new PreferenceAwareNotificationAdapter(
                delegate, preferencePort, clock, userPort, outbox, emailDigestHelper);
        Notification notification = Notification.create(
                userId, null, NotificationType.TASK_ASSIGNED, "TASK", 10L, "Task", "Assigned");

        adapter.save(notification);
        adapter.save(notification);

        ArgumentCaptor<NotificationEmailOutboxJpaEntity> captor =
                ArgumentCaptor.forClass(NotificationEmailOutboxJpaEntity.class);
        verify(emailDigestHelper, times(1)).create(captor.capture());
        assertEquals(notification.getSourceEventKey(), captor.getValue().getSourceEventKey());
    }

    @Test
    void loadsPreferenceOnceAndAggregatesDailyDigestForBothChannels() {
        NotificationRepositoryAdapter delegate = mock(NotificationRepositoryAdapter.class);
        LoadNotificationPreferencePort preferencePort = mock(LoadNotificationPreferencePort.class);
        LoadUserPort userPort = mock(LoadUserPort.class);
        SpringDataNotificationEmailOutboxRepository outbox = mock(SpringDataNotificationEmailOutboxRepository.class);
        TransactionalEmailDigestHelper emailDigestHelper = mock(TransactionalEmailDigestHelper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        UserId userId = new UserId(1L);
        NotificationPreference preference = NotificationPreference.createDefault(userId);
        preference.update(
                true, true,
                preference.getTaskAssignedChannel(), preference.getTaskDueReminderChannel(),
                preference.getTaskCommentChannel(), preference.getTimesheetReminderChannel(),
                preference.getAllocationChangedChannel(), preference.getScheduleConflictChannel(),
                NotificationFrequency.DAILY_DIGEST, 3, preference.getQuietHours());
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getUsername()).thenReturn("user");
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(userPort.findById(userId)).thenReturn(Optional.of(user));
        when(outbox.findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                any(), any(), any())).thenReturn(Optional.empty());
        NotificationEmailOutboxJpaEntity createdBatch = mock(NotificationEmailOutboxJpaEntity.class);
        when(createdBatch.getId()).thenReturn(42L);
        when(emailDigestHelper.create(any(NotificationEmailOutboxJpaEntity.class))).thenReturn(createdBatch);

        PreferenceAwareNotificationAdapter adapter = new PreferenceAwareNotificationAdapter(
                delegate, preferencePort, clock, userPort, outbox, emailDigestHelper);
        Notification notification = Notification.create(
                userId, null, NotificationType.TASK_ASSIGNED, "TASK", 10L, "Task", "Assigned");

        adapter.save(notification);

        verify(preferencePort, times(1)).findByUserId(userId);
        verify(delegate).appendToDigest(eq(notification), any(), eq(NotificationFrequency.DAILY_DIGEST),
                eq(clock.getZone()));
        verify(emailDigestHelper).create(any(NotificationEmailOutboxJpaEntity.class));
        verify(emailDigestHelper).appendIfAbsent(
                eq(42L), eq(notification.getSourceEventKey()), any(), any());
    }

    @Test
    void existingDigestBatchAppendsSourceOnlyOnceThroughDedupHelper() {
        NotificationRepositoryAdapter delegate = mock(NotificationRepositoryAdapter.class);
        LoadNotificationPreferencePort preferencePort = mock(LoadNotificationPreferencePort.class);
        LoadUserPort userPort = mock(LoadUserPort.class);
        SpringDataNotificationEmailOutboxRepository outbox = mock(SpringDataNotificationEmailOutboxRepository.class);
        TransactionalEmailDigestHelper emailDigestHelper = mock(TransactionalEmailDigestHelper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        UserId userId = new UserId(1L);
        NotificationPreference preference = NotificationPreference.createDefault(userId);
        preference.update(
                true, true,
                preference.getTaskAssignedChannel(), preference.getTaskDueReminderChannel(),
                preference.getTaskCommentChannel(), preference.getTimesheetReminderChannel(),
                preference.getAllocationChangedChannel(), preference.getScheduleConflictChannel(),
                NotificationFrequency.DAILY_DIGEST, 3, preference.getQuietHours());
        User user = mock(User.class);
        NotificationEmailOutboxJpaEntity batch = mock(NotificationEmailOutboxJpaEntity.class);
        when(batch.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getUsername()).thenReturn("user");
        when(preferencePort.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(userPort.findById(userId)).thenReturn(Optional.of(user));
        when(outbox.findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                any(), any(), any())).thenReturn(Optional.of(batch));

        PreferenceAwareNotificationAdapter adapter = new PreferenceAwareNotificationAdapter(
                delegate, preferencePort, clock, userPort, outbox, emailDigestHelper);
        Notification notification = Notification.create(
                userId, null, NotificationType.TASK_ASSIGNED, "TASK", 10L, "Task", "Assigned");

        adapter.save(notification);

        verify(emailDigestHelper).appendIfAbsent(
                eq(42L), eq(notification.getSourceEventKey()), any(), any());
        verify(emailDigestHelper, never()).append(any(), any());
    }
}
