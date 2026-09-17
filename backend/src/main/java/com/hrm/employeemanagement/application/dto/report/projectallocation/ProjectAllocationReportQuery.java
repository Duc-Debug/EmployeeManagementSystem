package com.hrm.employeemanagement.application.dto.report.projectallocation;

public record ProjectAllocationReportQuery(
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
) {
}