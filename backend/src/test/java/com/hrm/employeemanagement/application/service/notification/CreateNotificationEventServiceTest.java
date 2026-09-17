package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
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
}
