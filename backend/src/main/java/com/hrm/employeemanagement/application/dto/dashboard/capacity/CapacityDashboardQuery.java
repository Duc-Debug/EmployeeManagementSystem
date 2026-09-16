package com.hrm.employeemanagement.application.dto.dashboard.capacity;

import com.hrm.employeemanagement.domain.availability.YearWeek;

public record CapacityDashboardQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
    public CapacityDashboardQuery {
        if ((fromYear != null && fromWeek == null) || (fromYear == null && fromWeek != null)) {
            throw new IllegalArgumentException("fromYear và fromWeek phải được cung cấp đồng thời");
        }
        if (fromYear != null && fromWeek != null) {
            YearWeek.of(fromYear, fromWeek);
        }
        if (durationWeeks != null && (durationWeeks < 1 || durationWeeks > 52)) {
            throw new IllegalArgumentException("Số tuần phải từ 1 đến 52");
        }
    }
}
