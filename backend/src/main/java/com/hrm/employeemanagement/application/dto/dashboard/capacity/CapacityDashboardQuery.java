package com.hrm.employeemanagement.application.dto.dashboard.capacity;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;

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
        if (fromYear != null && (fromYear < 2000 || fromYear > 2100)) {
            throw new IllegalArgumentException("Năm bắt đầu phải nằm trong khoảng từ 2000 đến 2100");
        }
        if (fromWeek != null && (fromWeek < 1 || fromWeek > 53)) {
            throw new InvalidWeekNumberException("Số tuần bắt đầu phải nằm trong khoảng từ 1 đến 53");
        }
        if (durationWeeks != null && (durationWeeks < 1 || durationWeeks > 52)) {
            throw new IllegalArgumentException("Số tuần phải từ 1 đến 52");
        }
    }
}
