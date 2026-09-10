package com.hrm.employeemanagement.application.port.inbound.calendar;

import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;

import java.util.List;

public interface GetHolidaysUseCase {
    List<HolidayResult> getHolidaysByYear(int year);
}
