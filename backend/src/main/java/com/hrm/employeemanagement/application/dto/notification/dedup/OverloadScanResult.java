package com.hrm.employeemanagement.application.dto.notification.dedup;

public record OverloadScanResult(
        int totalScanned,
        int overloadedCount,
        int newlyAlertedCount,
        int skippedDedupCount,
        int resolvedCount
) {
    public static OverloadScanResult empty() {
        return new OverloadScanResult(0, 0, 0, 0, 0);
    }
}
