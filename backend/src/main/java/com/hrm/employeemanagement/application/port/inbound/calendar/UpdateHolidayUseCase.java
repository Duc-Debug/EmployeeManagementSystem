package com.hrm.employeemanagement.application.port.inbound.calendar;

import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;
import com.hrm.employeemanagement.application.dto.calendar.UpdateHolidayCommand;

public interface UpdateHolidayUseCase {
    HolidayResult updateHoliday(UpdateHolidayCommand command);
}
