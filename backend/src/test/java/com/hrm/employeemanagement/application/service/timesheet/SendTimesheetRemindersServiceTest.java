package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ProcessReminderBatchUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ReminderBatchResult;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendTimesheetRemindersServiceTest {

    @Mock private ProcessReminderBatchUseCase processReminderBatchUseCase;
    @Mock private LoadWorkingCalendarPort loadWorkingCalendarPort;
    @Mock private LoadHolidaysPort loadHolidaysPort;

    @Test
    void sendReminders_continuesAfterAFullyFailedPageToReachLaterEligibleTimesheets() {
        LocalDate lastWorkingDay = LocalDate.of(2026, 9, 18);
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(CompanyWorkingCalendar.createDefault());
        when(loadHolidaysPort.getHolidayDatesBetween(any(), any())).thenReturn(List.of());
        when(processReminderBatchUseCase.processBatch(lastWorkingDay, 500)).thenReturn(
                new ReminderBatchResult(500, 0, 0, 500),
                new ReminderBatchResult(200, 200, 0, 0),
                ReminderBatchResult.empty());

        new SendTimesheetRemindersService(processReminderBatchUseCase, loadWorkingCalendarPort, loadHolidaysPort,
                Clock.fixed(Instant.parse("2026-09-18T09:00:00Z"), ZoneOffset.UTC)).sendReminders();

        verify(processReminderBatchUseCase, times(3)).processBatch(lastWorkingDay, 500);
    }
}
