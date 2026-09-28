package com.hrm.employeemanagement.application.port.outbound.calendar;

import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;

public interface SaveWorkingCalendarPort {
    CompanyWorkingCalendar saveCompanyCalendar(CompanyWorkingCalendar calendar);
}
