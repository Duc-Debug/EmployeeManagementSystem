package com.hrm.employeemanagement.application.dto.notification;

public record GetNotificationCenterQuery(
        String status, // ALL, UNREAD, READ
        String level,  // ALL, CAO, TRUNG_BINH, THAP
        int page,
        int size
) {
    public GetNotificationCenterQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (status == null || status.isBlank()) {
            status = "ALL";
        }
        if (level == null || level.isBlank()) {
            level = "ALL";
        }
    }
}
