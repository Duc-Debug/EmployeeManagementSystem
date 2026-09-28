package com.hrm.employeemanagement.application.port.inbound.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;

public interface GetWorkingCalendarUseCase {
    CompanyWorkingCalendarResult getWorkingCalendar();
}
