package com.hrm.employeemanagement.application.dto.notification;

import java.util.Set;

public record GetNotificationCenterQuery(
        String status, // ALL, UNREAD, READ
        String level,  // ALL, CAO, TRUNG_BINH, THAP
        int page,
        int size
) {
    private static final Set<String> ALLOWED_STATUSES = Set.of("ALL", "UNREAD", "READ");
    private static final Set<String> ALLOWED_LEVELS = Set.of("ALL", "CAO", "TRUNG_BINH", "THAP");

    public GetNotificationCenterQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Chỉ số trang (page) không được nhỏ hơn 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Kích thước trang (size) phải từ 1 đến 100");
        }

        status = (status == null || status.isBlank()) ? "ALL" : status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái thông báo không hợp lệ: '" + status + "'. Giá trị hợp lệ: ALL, UNREAD, READ");
        }

        level = (level == null || level.isBlank()) ? "ALL" : level.trim().toUpperCase();
        if (!ALLOWED_LEVELS.contains(level)) {
            throw new IllegalArgumentException("Mức độ thông báo không hợp lệ: '" + level + "'. Giá trị hợp lệ: ALL, CAO, TRUNG_BINH, THAP");
        }
    }
}

