package com.hrm.employeemanagement.application.dto.report.projectallocation;

import com.hrm.employeemanagement.domain.availability.YearWeek;

public record ProjectAllocationReportQuery(
        Long projectId,
        Integer fromYear,
        Integer fromWeek,
        Integer toYear,
        Integer toWeek
) {
    public ProjectAllocationReportQuery {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId không được để trống");
        }

        if ((fromYear == null) != (fromWeek == null)) {
            throw new IllegalArgumentException("fromYear và fromWeek phải được cung cấp đồng thời");
        }

        if ((toYear == null) != (toWeek == null)) {
            throw new IllegalArgumentException("toYear và toWeek phải được cung cấp đồng thời");
        }

        YearWeek start = null;
        YearWeek end = null;

        if (fromYear != null && fromWeek != null) {
            start = YearWeek.of(fromYear, fromWeek);
        }

        if (toYear != null && toWeek != null) {
            end = YearWeek.of(toYear, toWeek);
        }

        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Thời gian kết thúc không được trước thời gian bắt đầu");
        }
    }
}