package com.hrm.employeemanagement.application.port.inbound.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CreateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;

public interface CreateHolidayUseCase {
    HolidayResult createHoliday(CreateHolidayCommand command);
}
