package com.hrm.employeemanagement.application.dto.workload;

public record GetUpcomingWorkloadQuery(
        Long employeeId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
    public GetUpcomingWorkloadQuery {
        if (durationWeeks == null) {
            durationWeeks = 8;
        }
    }
}
