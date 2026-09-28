package com.hrm.employeemanagement.application.dto.report.billablerate;

public record BillableRateQuery(
        Long orgUnitId,
        Long employeeId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
) {
}
