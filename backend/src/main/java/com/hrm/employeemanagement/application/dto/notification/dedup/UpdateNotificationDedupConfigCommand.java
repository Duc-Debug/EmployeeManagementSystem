package com.hrm.employeemanagement.application.dto.notification.dedup;

public record UpdateNotificationDedupConfigCommand(
        boolean isEnabled,
        int dedupWindowDays,
        int scanIntervalMinutes
) {
}
