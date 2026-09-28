package com.hrm.employeemanagement.application.dto.report.timesheetvariance;

/**
 * Query tham số lọc cho Báo cáo đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
public record TimesheetVarianceQuery(
        Long orgUnitId,
        Long employeeId,
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
) {
}
