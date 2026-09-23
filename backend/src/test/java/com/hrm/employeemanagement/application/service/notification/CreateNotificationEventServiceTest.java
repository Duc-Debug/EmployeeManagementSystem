package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEmailDeliveryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CreateNotificationEventService Tests (QTN-19, Idempotency & Soft-delete rule)")
class CreateNotificationEventServiceTest {

    private NotificationEventRepositoryPort eventRepo;
    private NotificationRecipientRepositoryPort recipientRepo;
    private CreateNotificationEventService service;

    @BeforeEach
    void setUp() {
        eventRepo = mock(NotificationEventRepositoryPort.class);
        recipientRepo = mock(NotificationRecipientRepositoryPort.class);
        service = new CreateNotificationEventService(eventRepo, recipientRepo);
    }

    @Test
    @DisplayName("QTN-19: Tạo sự kiện mới và fan-out cho 2 người nhận lần đầu")
    void createNewEventAndRecipients() {
        NotificationEvent createdEvent = new NotificationEvent(
                new NotificationEventId(50L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                LocalDateTime.now()
        );

        when(eventRepo.getOrCreate(any())).thenReturn(createdEvent);
        when(recipientRepo.findByEventIdAndRecipientUserId(eq(new NotificationEventId(50L)), any()))
                .thenReturn(Optional.empty());

        CreateNotificationEventCommand cmd = new CreateNotificationEventCommand(
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                List.of(101L, 102L)
        );

        Long eventId = service.execute(cmd);

        assertEquals(50L, eventId);
        // Verify đã tạo recipient cho cả 2 user
        verify(recipientRepo, times(2)).saveIfAbsent(any());
    }

    @Test
    @DisplayName("QTN-19: Gọi lại với cùng source_event_key và recipient đã có thì bỏ qua (không tạo trùng)")
    void qtn19_duplicateKeyDoesNotRecreate() {
        NotificationEvent existingEvent = new NotificationEvent(
                new NotificationEventId(50L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                LocalDateTime.now()
        );
        NotificationRecipientItem existingRecipient = new NotificationRecipientItem(
                new NotificationRecipientId(200L),
                new NotificationEventId(50L),
                new UserId(101L),
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(eventRepo.getOrCreate(any())).thenReturn(existingEvent);
        when(recipientRepo.findByEventIdAndRecipientUserId(new NotificationEventId(50L), new UserId(101L)))
                .thenReturn(Optional.of(existingRecipient));

        CreateNotificationEventCommand cmd = new CreateNotificationEventCommand(
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                List.of(101L)
        );

        Long eventId = service.execute(cmd);

        assertEquals(50L, eventId);
        // Không gọi save cho recipient vì đã có
        verify(recipientRepo, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("Phương án A: Recipient đã xóa mềm (is_deleted = true), khi event gửi lại thì KHÔNG tạo mới và KHÔNG restore")
    void softDeleteRule_noRecreateAndNoRestore() {
        NotificationEvent existingEvent = new NotificationEvent(
                new NotificationEventId(50L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                LocalDateTime.now()
        );
        NotificationRecipientItem softDeletedRecipient = new NotificationRecipientItem(
                new NotificationRecipientId(200L),
                new NotificationEventId(50L),
                new UserId(101L),
                true,
                LocalDateTime.now(),
                true, // Đã xóa mềm
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(eventRepo.getOrCreate(any())).thenReturn(existingEvent);
        when(recipientRepo.findByEventIdAndRecipientUserId(new NotificationEventId(50L), new UserId(101L)))
                .thenReturn(Optional.of(softDeletedRecipient));

        CreateNotificationEventCommand cmd = new CreateNotificationEventCommand(
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38",
                List.of(101L)
        );

        Long eventId = service.execute(cmd);

        assertEquals(50L, eventId);
        // Tôn trọng quyền xóa của user: không save tạo mới hay khôi phục
        verify(recipientRepo, never()).saveIfAbsent(any());
        assertTrue(softDeletedRecipient.isDeleted());
    }

    @Test
    void emailOnlyImmediateCreatesEmailWithoutInAppRecipient() {
        LoadNotificationPreferencePort preferences = mock(LoadNotificationPreferencePort.class);
        NotificationEmailDeliveryPort emailDelivery = mock(NotificationEmailDeliveryPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        NotificationEvent source = event(70L, "TASK_DUE_REMINDER", "SOURCE:EMAIL");
        NotificationPreference preference = preference(NotificationDeliveryChannel.EMAIL_ONLY,
                NotificationFrequency.IMMEDIATE);
        when(eventRepo.getOrCreate(any())).thenReturn(source);
        when(preferences.findByUserId(new UserId(101L))).thenReturn(Optional.of(preference));

        var canonicalService = new CreateNotificationEventService(
                eventRepo, recipientRepo, preferences, clock, emailDelivery);
        canonicalService.execute(command("TASK_DUE_REMINDER", "SOURCE:EMAIL"));

        verify(emailDelivery).schedule(eq(source), eq(new UserId(101L)), any());
        verify(recipientRepo, never()).saveIfAbsent(any());
    }

    @Test
    void inAppOnlyImmediateCreatesRecipientWithoutEmail() {
        LoadNotificationPreferencePort preferences = mock(LoadNotificationPreferencePort.class);
        NotificationEmailDeliveryPort emailDelivery = mock(NotificationEmailDeliveryPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        NotificationEvent source = event(71L, "TASK_DUE_REMINDER", "SOURCE:INAPP");
        NotificationPreference preference = preference(NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationFrequency.IMMEDIATE);
        when(eventRepo.getOrCreate(any())).thenReturn(source);
        when(preferences.findByUserId(new UserId(101L))).thenReturn(Optional.of(preference));
        when(recipientRepo.findByEventIdAndRecipientUserId(source.getId(), new UserId(101L)))
                .thenReturn(Optional.empty());

        var canonicalService = new CreateNotificationEventService(
                eventRepo, recipientRepo, preferences, clock, emailDelivery);
        canonicalService.execute(command("TASK_DUE_REMINDER", "SOURCE:INAPP"));

        verify(recipientRepo).saveIfAbsent(any());
        verify(emailDelivery, never()).schedule(any(), any(), any());
    }

    @Test
    void allDailyDigestCreatesBothChannelProjections() {
        LoadNotificationPreferencePort preferences = mock(LoadNotificationPreferencePort.class);
        NotificationEmailDeliveryPort emailDelivery = mock(NotificationEmailDeliveryPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        NotificationEvent source = event(72L, "TASK_DUE_REMINDER", "SOURCE:ALL");
        NotificationEvent digest = event(73L, "NOTIFICATION_DIGEST", "DIGEST:INAPP");
        NotificationPreference preference = preference(NotificationDeliveryChannel.ALL,
                NotificationFrequency.DAILY_DIGEST);
        when(eventRepo.getOrCreate(any())).thenReturn(source, digest);
        when(eventRepo.appendDigestItemIfAbsent(eq(digest), eq("SOURCE:ALL"), any(), any()))
                .thenReturn(digest);
        when(preferences.findByUserId(new UserId(101L))).thenReturn(Optional.of(preference));
        when(recipientRepo.findByEventIdAndRecipientUserId(digest.getId(), new UserId(101L)))
                .thenReturn(Optional.empty());

        var canonicalService = new CreateNotificationEventService(
                eventRepo, recipientRepo, preferences, clock, emailDelivery);
        canonicalService.execute(command("TASK_DUE_REMINDER", "SOURCE:ALL"));

        verify(emailDelivery).schedule(eq(source), eq(new UserId(101L)), argThat(d -> d.isDigest()));
        verify(eventRepo).appendDigestItemIfAbsent(eq(digest), eq("SOURCE:ALL"), any(), any());
        verify(recipientRepo).saveIfAbsent(any());
    }

    private NotificationPreference preference(
            NotificationDeliveryChannel taskDueChannel, NotificationFrequency frequency) {
        NotificationPreference preference = NotificationPreference.createDefault(new UserId(101L));
        preference.update(true, true,
                NotificationDeliveryChannel.ALL, taskDueChannel,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                frequency, 3, preference.getQuietHours());
        return preference;
    }

    private NotificationEvent event(Long id, String type, String sourceKey) {
        return new NotificationEvent(new NotificationEventId(id), type, NotificationLevel.THAP,
                "Title", "Message", "TASK", "10", sourceKey, LocalDateTime.now());
    }

    private CreateNotificationEventCommand command(String type, String sourceKey) {
        return new CreateNotificationEventCommand(type, NotificationLevel.THAP, "Title", "Message",
                "TASK", "10", sourceKey, List.of(101L));
    }
}
