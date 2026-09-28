package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PurgeExpiredNotificationsService Tests (TC-05, Retention 45 days hard delete)")
class PurgeExpiredNotificationsServiceTest {

    private NotificationRecipientRepositoryPort recipientRepo;
    private NotificationEventRepositoryPort eventRepo;
    private PurgeExpiredNotificationsService service;

    @BeforeEach
    void setUp() {
        recipientRepo = mock(NotificationRecipientRepositoryPort.class);
        eventRepo = mock(NotificationEventRepositoryPort.class);
        service = new PurgeExpiredNotificationsService(recipientRepo, eventRepo);
    }

    @Test
    @DisplayName("TC-05: Quét và hard delete các recipient và event quá 45 ngày")
    void tc05_purgeNotificationsOlderThan45Days() {
        when(recipientRepo.purgeRecipientsOlderThan(any())).thenReturn(10L);
        when(eventRepo.purgeOrphanEventsOlderThan(any())).thenReturn(5L);

        long totalPurged = service.execute();

        assertEquals(15L, totalPurged);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.now().getClass());
        verify(recipientRepo, times(1)).purgeRecipientsOlderThan(cutoffCaptor.capture());
        verify(eventRepo, times(1)).purgeOrphanEventsOlderThan(cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        // Cutoff xấp xỉ 45 ngày trước
        assertTrue(cutoff.isBefore(LocalDateTime.now().minusDays(44)));
        assertTrue(cutoff.isAfter(LocalDateTime.now().minusDays(46)));
    }
}
