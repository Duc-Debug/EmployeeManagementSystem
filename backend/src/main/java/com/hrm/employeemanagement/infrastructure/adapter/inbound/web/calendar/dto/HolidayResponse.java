package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;

import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        LocalDate holidayDate,
        String name,
        int workingHoursDeducted
) {
    public static HolidayResponse from(HolidayResult result) {
        return new HolidayResponse(
                result.id(),
                result.holidayDate(),
                result.name(),
                result.workingHoursDeducted()
        );
    }
}
