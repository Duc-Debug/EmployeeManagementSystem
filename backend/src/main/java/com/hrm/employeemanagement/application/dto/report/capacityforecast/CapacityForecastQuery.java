package com.hrm.employeemanagement.application.dto.report.capacityforecast;

/**
 * DTO chứa thông tin truy vấn báo cáo dự báo năng lực các tuần tới (NCL-10-CN-004).
 */
public record CapacityForecastQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks
) {
}

