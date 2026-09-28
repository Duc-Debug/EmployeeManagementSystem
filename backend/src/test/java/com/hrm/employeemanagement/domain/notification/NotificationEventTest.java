package com.hrm.employeemanagement.domain.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationEvent Domain Aggregate Tests")
class NotificationEventTest {

    @Test
    @DisplayName("Khởi tạo NotificationEvent hợp lệ")
    void createValidEvent() {
        NotificationEvent event = NotificationEvent.create(
                "OVERLOAD_WARNING",
                NotificationLevel.CAO,
                "Cảnh báo quá tải",
                "Chi tiết cảnh báo",
                "CAPACITY_WEEK",
                "2026-W38",
                "OVERLOAD:E001:2026-W38"
        );

        assertNotNull(event);
        assertEquals("OVERLOAD_WARNING", event.getEventType());
        assertEquals(NotificationLevel.CAO, event.getLevel());
        assertEquals("Cảnh báo quá tải", event.getTitle());
        assertEquals("Chi tiết cảnh báo", event.getMessage());
        assertEquals("CAPACITY_WEEK", event.getRelatedEntityType());
        assertEquals("2026-W38", event.getRelatedEntityId());
        assertEquals("OVERLOAD:E001:2026-W38", event.getSourceEventKey());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Ném exception khi thiếu trường bắt buộc")
    void failWhenRequiredFieldsMissing() {
        assertThrows(NullPointerException.class, () ->
                NotificationEvent.create(null, NotificationLevel.CAO, "Title", "Msg", null, null, "KEY"));

        assertThrows(NullPointerException.class, () ->
                NotificationEvent.create("TYPE", NotificationLevel.CAO, null, "Msg", null, null, "KEY"));

        assertThrows(NullPointerException.class, () ->
                NotificationEvent.create("TYPE", NotificationLevel.CAO, "Title", "Msg", null, null, null));
    }
}
