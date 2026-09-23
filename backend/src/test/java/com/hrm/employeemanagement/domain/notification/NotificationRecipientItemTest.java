package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationRecipientItem Domain Entity Tests")
class NotificationRecipientItemTest {

    @Test
    @DisplayName("Khởi tạo NotificationRecipientItem ban đầu chưa đọc và chưa xóa")
    void createInitialItem() {
        NotificationRecipientItem item = NotificationRecipientItem.create(
                new NotificationEventId(10L),
                new UserId(100L)
        );

        assertNotNull(item);
        assertEquals(10L, item.getEventId().value());
        assertEquals(100L, item.getRecipientUserId().value());
        assertFalse(item.isRead());
        assertNull(item.getReadAt());
        assertFalse(item.isDeleted());
        assertNull(item.getDeletedAt());
        assertNotNull(item.getCreatedAt());
    }

    @Test
    @DisplayName("Chuyển trạng thái markAsRead cập nhật isRead và readAt")
    void markAsReadUpdatesState() {
        NotificationRecipientItem item = NotificationRecipientItem.create(
                new NotificationEventId(10L),
                new UserId(100L)
        );

        LocalDateTime now = LocalDateTime.now();
        item.markAsRead(now);

        assertTrue(item.isRead());
        assertEquals(now, item.getReadAt());
    }

    @Test
    @DisplayName("Chuyển trạng thái softDelete cập nhật isDeleted và deletedAt")
    void softDeleteUpdatesState() {
        NotificationRecipientItem item = NotificationRecipientItem.create(
                new NotificationEventId(10L),
                new UserId(100L)
        );

        LocalDateTime now = LocalDateTime.now();
        item.softDelete(now);

        assertTrue(item.isDeleted());
        assertEquals(now, item.getDeletedAt());
    }
}
