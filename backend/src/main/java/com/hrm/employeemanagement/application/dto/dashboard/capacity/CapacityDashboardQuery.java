package com.hrm.employeemanagement.application.dto.dashboard.capacity;

public record CapacityDashboardQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
    public CapacityDashboardQuery {
        if (durationWeeks != null && (durationWeeks < 1 || durationWeeks > 52)) {
            throw new IllegalArgumentException("Số tuần phải từ 1 đến 52");
        }
    }
}
