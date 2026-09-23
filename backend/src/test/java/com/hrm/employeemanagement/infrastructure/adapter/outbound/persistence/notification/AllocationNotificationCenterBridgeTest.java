package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.notification.*;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AllocationNotificationCenterBridgeTest {
    @Test
    void publishesAllocationToBellWithCorrectProjectRecipientAndDeliveryTime() {
        var legacy = mock(SpringDataNotificationRepository.class);
        var mapper = mock(NotificationPersistenceMapper.class);
        var events = mock(NotificationEventRepositoryPort.class);
        var recipients = mock(NotificationRecipientRepositoryPort.class);
        var adapter = new NotificationRepositoryAdapter(legacy, mapper,
                mock(TransactionalLegacyDigestHelper.class), events, recipients);
        var availableAt = LocalDateTime.of(2026, 9, 23, 8, 0);
        var notification = Notification.create(new UserId(20L), new UserId(30L),
                NotificationType.ALLOCATION_CHANGED, "PROJECT_ALLOCATION", 100L,
                "Điều chỉnh phân bổ", "Đổi giờ tuần 39").scheduledFor(availableAt);
        var entity = mock(NotificationJpaEntity.class);
        when(mapper.toJpaEntity(notification)).thenReturn(entity);
        when(legacy.save(entity)).thenReturn(entity);
        when(entity.getId()).thenReturn(500L);
        var savedEvent = mock(NotificationEvent.class);
        when(savedEvent.getId()).thenReturn(new NotificationEventId(600L));
        when(events.save(any())).thenReturn(savedEvent);

        adapter.save(notification);

        var event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("ALLOCATION_CHANGED");
        assertThat(event.getValue().getRelatedEntityId()).isEqualTo("100");
        assertThat(event.getValue().getSourceEventKey()).isEqualTo("LEGACY:NOTIF:500");
        var recipient = ArgumentCaptor.forClass(NotificationRecipientItem.class);
        verify(recipients).save(recipient.capture());
        assertThat(recipient.getValue().getRecipientUserId()).isEqualTo(new UserId(20L));
        assertThat(recipient.getValue().getAvailableAt()).isEqualTo(availableAt);
        verify(events, never()).getOrCreate(any());
        verify(recipients, never()).saveIfAbsent(any());
    }
}
