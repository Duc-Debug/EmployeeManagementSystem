package com.hrm.employeemanagement.application.dto.allocation;

import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;

public record CompanyWeeklyCapacityQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks,
        Integer page,
        Integer size,
        String search,
        CapacityStatus status
) {
    public CompanyWeeklyCapacityQuery {
        if (durationWeeks == null || durationWeeks <= 0) {
            durationWeeks = 8;
        }
        if (durationWeeks > 16) {
            durationWeeks = 16;
        }
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        } else if (size > 50) {
            size = 50;
        }
        if (search != null) {
            search = search.trim();
            if (search.isEmpty()) {
                search = null;
            }
        }

        // [HIGH REVIEW FIX]: Validate (fromYear, fromWeek) combination at the application boundary
        if (fromYear != null && fromWeek != null) {
            YearWeek.of(fromYear, fromWeek);
        } else if ((fromYear != null && fromWeek == null) || (fromYear == null && fromWeek != null)) {
            throw new InvalidWeekNumberException("fromYear và fromWeek phải cùng được cung cấp hoặc cùng để trống");
        }
    }

    public CompanyWeeklyCapacityQuery(Long orgUnitId, Integer fromYear, Integer fromWeek, Integer durationWeeks) {
        this(orgUnitId, fromYear, fromWeek, durationWeeks, 0, 20, null, null);
    }

    public CompanyWeeklyCapacityQuery(Long orgUnitId, Integer fromYear, Integer fromWeek, Integer durationWeeks, Integer page, Integer size, String search) {
        this(orgUnitId, fromYear, fromWeek, durationWeeks, page, size, search, null);
    }
}
