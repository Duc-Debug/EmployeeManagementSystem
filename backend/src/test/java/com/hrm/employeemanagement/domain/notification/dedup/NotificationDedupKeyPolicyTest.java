package com.hrm.employeemanagement.domain.notification.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDedupKeyPolicyTest {

    @Test
    @DisplayName("Khóa chống trùng chuẩn hóa đúng cấu trúc: {eventType}:{targetEntityType}:{targetEntityId}:{yearWeek}:USER:{recipientUserId}")
    void testBuildKey_Success() {
        String key = NotificationDedupKeyPolicy.buildKey(
                "OVERLOAD_WARNING",
                "EMPLOYEE",
                "101",
                "2026-W38",
                201L
        );
        assertEquals("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201", key);
    }

    @Test
    @DisplayName("Tự động chuẩn hóa chữ hoa và loại bỏ khoảng trắng thừa")
    void testBuildKey_NormalizeAndTrim() {
        String key = NotificationDedupKeyPolicy.buildKey(
                "  overload_warning  ",
                " employee ",
                " 101 ",
                " 2026-W38 ",
                201L
        );
        assertEquals("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201", key);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi thiếu các thành phần bắt buộc của khóa")
    void testBuildKey_ValidationExceptions() {
        assertThrows(IllegalArgumentException.class, () ->
                NotificationDedupKeyPolicy.buildKey(null, "EMPLOYEE", "101", "2026-W38", 201L));

        assertThrows(IllegalArgumentException.class, () ->
                NotificationDedupKeyPolicy.buildKey("OVERLOAD_WARNING", "", "101", "2026-W38", 201L));

        assertThrows(IllegalArgumentException.class, () ->
                NotificationDedupKeyPolicy.buildKey("OVERLOAD_WARNING", "EMPLOYEE", "  ", "2026-W38", 201L));

        assertThrows(IllegalArgumentException.class, () ->
                NotificationDedupKeyPolicy.buildKey("OVERLOAD_WARNING", "EMPLOYEE", "101", null, 201L));

        assertThrows(NullPointerException.class, () ->
                NotificationDedupKeyPolicy.buildKey("OVERLOAD_WARNING", "EMPLOYEE", "101", "2026-W38", null));
    }
}
