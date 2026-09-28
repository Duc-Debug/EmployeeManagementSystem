package com.hrm.employeemanagement.infrastructure.transaction.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;
import com.hrm.employeemanagement.application.dto.calendar.CreateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;
import com.hrm.employeemanagement.application.dto.calendar.UpdateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;
import com.hrm.employeemanagement.application.port.inbound.calendar.CreateHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.DeleteHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetHolidaysUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetWorkingCalendarUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateWorkingCalendarUseCase;
import com.hrm.employeemanagement.application.service.calendar.WorkingCalendarService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

public class TransactionalWorkingCalendarServiceDecorator implements
        GetWorkingCalendarUseCase,
        UpdateWorkingCalendarUseCase,
        GetHolidaysUseCase,
        CreateHolidayUseCase,
        UpdateHolidayUseCase,
        DeleteHolidayUseCase {

    private final WorkingCalendarService delegate;

    public TransactionalWorkingCalendarServiceDecorator(WorkingCalendarService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "WorkingCalendarService must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyWorkingCalendarResult getWorkingCalendar() {
        return delegate.getWorkingCalendar();
    }

    @Override
    @Transactional
    public CompanyWorkingCalendarResult updateWorkingCalendar(List<WorkingCalendarDayDto> days) {
        return delegate.updateWorkingCalendar(days);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResult> getHolidaysByYear(int year) {
        return delegate.getHolidaysByYear(year);
    }

    @Override
    @Transactional
    public HolidayResult createHoliday(CreateHolidayCommand command) {
        return delegate.createHoliday(command);
    }

    @Override
    @Transactional
    public HolidayResult updateHoliday(UpdateHolidayCommand command) {
        return delegate.updateHoliday(command);
    }

    @Override
    @Transactional
    public void deleteHoliday(Long holidayId) {
        delegate.deleteHoliday(holidayId);
    }
}
