package com.hrm.employeemanagement.application.service.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.dto.notification.GetNotificationCenterQuery;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterItemResult;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterPageResult;
import com.hrm.employeemanagement.application.dto.notification.UnreadNotificationCountResult;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationAuditLogRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationEventRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.NotificationRecipientRepositoryPort;
import com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException;
import com.hrm.employeemanagement.domain.exception.notification.NotificationNotFoundException;
import com.hrm.employeemanagement.domain.notification.NotificationAuditAction;
import com.hrm.employeemanagement.domain.notification.NotificationAuditLog;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientId;
import com.hrm.employeemanagement.domain.notification.NotificationRecipientItem;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("NotificationCenterApplicationService Tests (NCL-11-CN-001, TC-01 -> TC-04)")
class NotificationCenterApplicationServiceTest {

    private NotificationRecipientRepositoryPort recipientRepo;
    private NotificationEventRepositoryPort eventRepo;
    private NotificationAuditLogRepositoryPort auditRepo;
    private NotificationCenterApplicationService service;

    private final Long currentUserId = 100L;
    private final Long otherUserId = 200L;

    @BeforeEach
    void setUp() {
        recipientRepo = mock(NotificationRecipientRepositoryPort.class);
        eventRepo = mock(NotificationEventRepositoryPort.class);
        auditRepo = mock(NotificationAuditLogRepositoryPort.class);
        service = new NotificationCenterApplicationService(recipientRepo, eventRepo, auditRepo);
    }

    @Test
    @DisplayName("TC-01: Lấy danh sách thông báo phân trang và đếm unread count thành công")
    void tc01_getNotificationsAndUnreadCount() {
        NotificationEvent event = new NotificationEvent(
                new NotificationEventId(1L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Quá tải 48h",
                "CAPACITY_WEEK",
                "2026-W38",
                "KEY:01",
                LocalDateTime.now()
        );
        NotificationRecipientItem recipient = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(currentUserId),
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        GetNotificationCenterQuery query = new GetNotificationCenterQuery("ALL", "ALL", 0, 20);

        when(recipientRepo.findRecipients(new UserId(currentUserId), "ALL", "ALL", 0, 20))
                .thenReturn(List.of(recipient));
        when(recipientRepo.countRecipients(new UserId(currentUserId), "ALL", "ALL")).thenReturn(1L);
        when(recipientRepo.countUnread(new UserId(currentUserId))).thenReturn(1L);
        when(eventRepo.findById(new NotificationEventId(1L))).thenReturn(Optional.of(event));

        NotificationCenterPageResult result = service.getNotifications(currentUserId, query);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(1L, result.unreadCount());

        NotificationCenterItemResult item = result.items().get(0);
        assertEquals(10L, item.id());
        assertEquals(1L, item.eventId());
        assertEquals("OVERLOAD_WARNING", item.eventType());
        assertEquals(NotificationLevel.CAO, item.level());
        assertFalse(item.isRead());

        UnreadNotificationCountResult unreadResult = service.getUnreadCount(currentUserId);
        assertEquals(1L, unreadResult.unreadCount());
    }

    @Test
    @DisplayName("TC-02: Xem chi tiết thông báo đọc thuần túy, KHÔNG thay đổi isRead và KHÔNG ghi audit log")
    void tc02_getNotificationDetailPureRead() {
        NotificationEvent event = new NotificationEvent(
                new NotificationEventId(1L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Quá tải 48h",
                "CAPACITY_WEEK",
                "2026-W38",
                "KEY:01",
                LocalDateTime.now()
        );
        NotificationRecipientItem recipient = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(currentUserId),
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(recipientRepo.findById(NotificationRecipientId.of(10L))).thenReturn(Optional.of(recipient));
        when(eventRepo.findById(new NotificationEventId(1L))).thenReturn(Optional.of(event));

        NotificationCenterItemResult detail = service.getNotificationDetail(10L, currentUserId);

        assertNotNull(detail);
        assertEquals(10L, detail.id());
        assertEquals("CAPACITY_WEEK", detail.relatedEntityType());
        assertEquals("2026-W38", detail.relatedEntityId());
        assertFalse(detail.isRead()); // Vẫn chưa đọc

        // Verify không có mutation save và không có audit log
        verify(recipientRepo, never()).save(any());
        verify(auditRepo, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Đánh dấu 1 thông báo đã đọc cập nhật isRead và ghi audit log MARK_READ")
    void tc03_markAsReadSuccess() {
        NotificationRecipientItem recipient = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(currentUserId),
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(recipientRepo.findById(NotificationRecipientId.of(10L))).thenReturn(Optional.of(recipient));

        service.markAsRead(10L, currentUserId);

        assertTrue(recipient.isRead());
        assertNotNull(recipient.getReadAt());
        verify(recipientRepo, times(1)).save(recipient);

        ArgumentCaptor<NotificationAuditLog> auditCaptor = ArgumentCaptor.forClass(NotificationAuditLog.class);
        verify(auditRepo, times(1)).save(auditCaptor.capture());

        NotificationAuditLog audit = auditCaptor.getValue();
        assertEquals(currentUserId, audit.getActorUserId().value());
        assertEquals(NotificationAuditAction.MARK_READ, audit.getAction());
        assertEquals("NOTIFICATION_RECIPIENT", audit.getTargetType());
        assertEquals(10L, audit.getTargetId());
        assertTrue(audit.getDetail().contains("\"previousIsRead\":false"));
    }

    @Test
    @DisplayName("TC-03: Đánh dấu tất cả đã đọc ghi 1 audit log MARK_ALL_READ với affectedCount")
    void tc03_markAllAsReadSuccess() {
        when(recipientRepo.markAllAsRead(eq(new UserId(currentUserId)), any())).thenReturn(5);

        service.markAllAsRead(currentUserId);

        verify(recipientRepo, times(1)).markAllAsRead(eq(new UserId(currentUserId)), any());

        ArgumentCaptor<NotificationAuditLog> auditCaptor = ArgumentCaptor.forClass(NotificationAuditLog.class);
        verify(auditRepo, times(1)).save(auditCaptor.capture());

        NotificationAuditLog audit = auditCaptor.getValue();
        assertEquals(currentUserId, audit.getActorUserId().value());
        assertEquals(NotificationAuditAction.MARK_ALL_READ, audit.getAction());
        assertEquals("NOTIFICATION_CENTER", audit.getTargetType());
        assertNull(audit.getTargetId()); // Semantics chuẩn xác NULL cho toàn inbox
        assertTrue(audit.getDetail().contains("\"affectedCount\":5"));
    }

    @Test
    @DisplayName("TC-04: Xóa mềm thông báo, cập nhật isDeleted và ghi audit snapshot đầy đủ")
    void tc04_deleteNotificationSoftDeleteWithSnapshot() {
        NotificationEvent event = new NotificationEvent(
                new NotificationEventId(1L),
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Nội dung quá tải",
                "CAPACITY_WEEK",
                "2026-W38",
                "KEY:01",
                LocalDateTime.now()
        );
        NotificationRecipientItem recipient = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(currentUserId),
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(recipientRepo.findById(NotificationRecipientId.of(10L))).thenReturn(Optional.of(recipient));
        when(eventRepo.findById(new NotificationEventId(1L))).thenReturn(Optional.of(event));

        service.deleteNotification(10L, currentUserId);

        assertTrue(recipient.isDeleted());
        assertNotNull(recipient.getDeletedAt());
        verify(recipientRepo, times(1)).save(recipient);

        ArgumentCaptor<NotificationAuditLog> auditCaptor = ArgumentCaptor.forClass(NotificationAuditLog.class);
        verify(auditRepo, times(1)).save(auditCaptor.capture());

        NotificationAuditLog audit = auditCaptor.getValue();
        assertEquals(currentUserId, audit.getActorUserId().value());
        assertEquals(NotificationAuditAction.DELETE_NOTIFICATION, audit.getAction());
        assertEquals("NOTIFICATION_RECIPIENT", audit.getTargetType());
        assertEquals(10L, audit.getTargetId());
        assertTrue(audit.getDetail().contains("Cảnh báo quá tải"));
        assertTrue(audit.getDetail().contains("OVERLOAD_WARNING"));
        assertTrue(audit.getDetail().contains("CAPACITY_WEEK"));
    }

    @Test
    @DisplayName("Security Boundary: User A thao tác trên thông báo của User B bị ném NotificationAccessDeniedException")
    void securityBoundary_otherUserCannotAccess() {
        NotificationRecipientItem recipientOfUserB = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(otherUserId), // Thuộc User B
                false,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(recipientRepo.findById(NotificationRecipientId.of(10L))).thenReturn(Optional.of(recipientOfUserB));

        // User A (currentUserId) cố đọc chi tiết
        assertThrows(NotificationAccessDeniedException.class, () ->
                service.getNotificationDetail(10L, currentUserId));

        // User A cố mark as read
        assertThrows(NotificationAccessDeniedException.class, () ->
                service.markAsRead(10L, currentUserId));

        // User A cố xóa
        assertThrows(NotificationAccessDeniedException.class, () ->
                service.deleteNotification(10L, currentUserId));

        verify(auditRepo, never()).save(any());
    }

    @Test
    @DisplayName("Truy cập thông báo không tồn tại hoặc đã bị xóa mềm ném NotificationNotFoundException")
    void notFoundOrDeletedThrowsException() {
        when(recipientRepo.findById(NotificationRecipientId.of(99L))).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () ->
                service.getNotificationDetail(99L, currentUserId));

        NotificationRecipientItem deletedItem = new NotificationRecipientItem(
                new NotificationRecipientId(10L),
                new NotificationEventId(1L),
                new UserId(currentUserId),
                false,
                null,
                true, // Đã xóa mềm
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(recipientRepo.findById(NotificationRecipientId.of(10L))).thenReturn(Optional.of(deletedItem));

        assertThrows(NotificationNotFoundException.class, () ->
                service.getNotificationDetail(10L, currentUserId));
    }
}
