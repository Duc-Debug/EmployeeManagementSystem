package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.util.List;

/**
 * NCL-06-CN-006: DTO trả về kết quả phân bổ hàng loạt gồm tổng số tuần, số tuần thành công và danh sách các tuần bị chặn kèm lý do.
 */
public record BulkAllocationResult(
        Long employeeId,
        Long projectId,
        int totalRequestedWeeks,
        int successCount,
        int blockedCount,
        List<AllocatedWeekSummary> successWeeks,
        List<BlockedWeekSummary> blockedWeeks
) {
    public record AllocatedWeekSummary(
            int year,
            int weekNumber,
            BigDecimal allocatedHours,
            BigDecimal remainingHours
    ) {}

    public record BlockedWeekSummary(
            int year,
            int weekNumber,
            String reasonCode,
            String reasonMessage,
            BigDecimal netAvailableHours,
            BigDecimal currentAllocatedHours,
            BigDecimal requestedHours
    ) {}
}
