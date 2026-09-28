package com.hrm.employeemanagement.application.port.inbound.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;
import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;

import java.util.List;

public interface UpdateWorkingCalendarUseCase {
    CompanyWorkingCalendarResult updateWorkingCalendar(List<WorkingCalendarDayDto> days);
}
