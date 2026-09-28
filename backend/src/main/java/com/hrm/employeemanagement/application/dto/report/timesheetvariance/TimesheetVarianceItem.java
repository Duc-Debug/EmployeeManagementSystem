package com.hrm.employeemanagement.application.dto.report.timesheetvariance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bản ghi chi tiết đối chiếu theo từng nhân sự / dự án / tuần.
 */
public record TimesheetVarianceItem(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String orgUnitName,
        Long projectId,
        String projectName,
        Integer year,
        Integer weekNumber,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        BigDecimal allocatedHours,
        BigDecimal actualApprovedHours,
        BigDecimal varianceHours,
        BigDecimal variancePercentage,
        Boolean hasActualData,
        String varianceStatus
) {
}
