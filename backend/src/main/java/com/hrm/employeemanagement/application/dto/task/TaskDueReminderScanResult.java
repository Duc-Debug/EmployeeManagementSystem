package com.hrm.employeemanagement.application.dto.task;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO kết quả của tác vụ rà soát công việc sắp đến hạn (NCL-11-CN-004).
 */
public record TaskDueReminderScanResult(
        LocalDate scanDate,
        int totalScanned,
        int sentCount,
        int skippedDuplicateCount,
        int skippedCompletedCount,
        List<Long> notifiedTaskIds
) {
    public static TaskDueReminderScanResult empty(LocalDate scanDate) {
        return new TaskDueReminderScanResult(scanDate, 0, 0, 0, 0, List.of());
    }
}
