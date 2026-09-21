package com.hrm.employeemanagement.domain.notification.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDedupConfigTest {

    @Test
    @DisplayName("Khởi tạo cấu hình mặc định hợp lệ")
    void testDefaultConfig() {
        NotificationDedupConfig config = NotificationDedupConfig.defaultConfig();
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertEquals(7, config.getDedupWindowDays());
        assertEquals(60, config.getScanIntervalMinutes());
        assertEquals(0L, config.getVersion());
    }

    @Test
    @DisplayName("Cập nhật cấu hình thành công với các tham số hợp lệ")
    void testUpdate_Success() {
        NotificationDedupConfig config = NotificationDedupConfig.defaultConfig();
        LocalDateTime now = LocalDateTime.now();

        config.update(false, 14, 120, 99L, now);

        assertEquals(false, config.isEnabled());
        assertEquals(14, config.getDedupWindowDays());
        assertEquals(120, config.getScanIntervalMinutes());
        assertEquals(99L, config.getUpdatedBy());
        assertEquals(now, config.getUpdatedAt());
    }

    @Test
    @DisplayName("Kiểm tra giá trị biên cho cửa sổ chống trùng (1 đến 90 ngày)")
    void testWindowDays_Boundaries() {
        NotificationDedupConfig config = NotificationDedupConfig.defaultConfig();

        // Biên hợp lệ
        config.update(true, 1, 60, 1L, LocalDateTime.now());
        assertEquals(1, config.getDedupWindowDays());

        config.update(true, 90, 60, 1L, LocalDateTime.now());
        assertEquals(90, config.getDedupWindowDays());

        // Biên không hợp lệ
        assertThrows(IllegalArgumentException.class, () ->
                config.update(true, 0, 60, 1L, LocalDateTime.now()));

        assertThrows(IllegalArgumentException.class, () ->
                config.update(true, 91, 60, 1L, LocalDateTime.now()));
    }

    @Test
    @DisplayName("Kiểm tra giá trị biên cho chu kỳ quét (5 đến 1440 phút)")
    void testScanInterval_Boundaries() {
        NotificationDedupConfig config = NotificationDedupConfig.defaultConfig();

        // Biên hợp lệ
        config.update(true, 7, 5, 1L, LocalDateTime.now());
        assertEquals(5, config.getScanIntervalMinutes());

        config.update(true, 7, 1440, 1L, LocalDateTime.now());
        assertEquals(1440, config.getScanIntervalMinutes());

        // Biên không hợp lệ
        assertThrows(IllegalArgumentException.class, () ->
                config.update(true, 7, 4, 1L, LocalDateTime.now()));

        assertThrows(IllegalArgumentException.class, () ->
                config.update(true, 7, 1441, 1L, LocalDateTime.now()));
    }
}
