package com.hrm.employeemanagement.domain.notification;

/**
 * 3 mức độ thông báo theo đặc tả NCL-11-CN-001 (Quy tắc mức độ đã chốt):
 * - CAO: Màu đỏ - Cảnh báo cần xử lý ngay (quá tải, xung đột nghiêm trọng)
 * - TRUNG_BINH: Màu vàng - Cần lưu ý, chưa khẩn cấp
 * - THAP: Màu xanh - Thông tin tham khảo
 */
public enum NotificationLevel {
    CAO,
    TRUNG_BINH,
    THAP;

    public static NotificationLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return THAP;
        }
        try {
            return NotificationLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return THAP;
        }
    }
}
