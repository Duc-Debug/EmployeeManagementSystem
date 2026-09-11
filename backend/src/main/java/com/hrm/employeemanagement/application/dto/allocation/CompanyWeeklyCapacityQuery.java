package com.hrm.employeemanagement.application.dto.allocation;

public record CompanyWeeklyCapacityQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
    public CompanyWeeklyCapacityQuery {
        if (durationWeeks == null || durationWeeks <= 0) {
            durationWeeks = 8;
        }
        if (durationWeeks > 52) {
            durationWeeks = 52;
        }
    }
}
