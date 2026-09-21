package com.hrm.employeemanagement.application.dto.notification.dedup;

import java.time.LocalDateTime;

public record NotificationDedupConfigResult(
        boolean isEnabled,
        int dedupWindowDays,
        int scanIntervalMinutes,
        Long updatedBy,
        LocalDateTime updatedAt,
        Long version
) {
}
