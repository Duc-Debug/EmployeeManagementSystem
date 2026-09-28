package com.hrm.employeemanagement.application.dto.allocation;

import java.util.List;

public record AllocationNotificationPageResult(
        List<AllocationNotificationItemResult> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
