package com.hrm.employeemanagement.application.dto.report;

public record RecruitmentDemandReportQuery(
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek,
        Long orgUnitId
) {
}
