package com.hrm.employeemanagement.application.dto.allocation.idleness;

import java.math.BigDecimal;
import java.util.List;

/**
 * Báo cáo tổng thể danh sách nhân sự nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23).
 */
public record ProlongedIdlenessReportResult(
        Long orgUnitId,
        String orgUnitName,
        int fromYear,
        int fromWeek,
        int durationWeeks,
        BigDecimal effectiveIdleThreshold,
        int consecutiveThreshold,
        int totalIdleEmployees,
        int page,
        int size,
        int totalPages,
        List<ProlongedIdleStaffItemResult> items
) {
    public ProlongedIdlenessReportResult(
            Long orgUnitId,
            String orgUnitName,
            int fromYear,
            int fromWeek,
            int durationWeeks,
            BigDecimal effectiveIdleThreshold,
            int consecutiveThreshold,
            int totalIdleEmployees,
            List<ProlongedIdleStaffItemResult> items
    ) {
        this(
                orgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                effectiveIdleThreshold,
                consecutiveThreshold,
                totalIdleEmployees,
                0,
                items != null && !items.isEmpty() ? items.size() : 20,
                1,
                items
        );
    }
}
