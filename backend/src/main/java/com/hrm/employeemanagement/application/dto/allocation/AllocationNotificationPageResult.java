package com.hrm.employeemanagement.application.dto.allocation;

import java.util.List;

public record AllocationNotificationPageResult(
        List<AllocationNotificationItemResult> items,
        long totalElements,
        int totalPages,
        int currentPage
) {
}
