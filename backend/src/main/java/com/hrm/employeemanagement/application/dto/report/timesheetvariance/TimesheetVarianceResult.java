package com.hrm.employeemanagement.application.dto.report.timesheetvariance;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả trả về của Use Case Báo cáo đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
public record TimesheetVarianceResult(
        Long orgUnitId,
        String orgUnitName,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        List<TimesheetVarianceItem> items,
        TimesheetVarianceSummary summary,
        Boolean hasAnyActualData,
        String message,
        LocalDateTime generatedAt
) {
}
