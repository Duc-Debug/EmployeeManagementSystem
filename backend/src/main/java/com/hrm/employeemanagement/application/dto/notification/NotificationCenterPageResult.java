package com.hrm.employeemanagement.application.dto.notification;

import java.util.List;

public record NotificationCenterPageResult(
        List<NotificationCenterItemResult> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        long unreadCount
) {}
