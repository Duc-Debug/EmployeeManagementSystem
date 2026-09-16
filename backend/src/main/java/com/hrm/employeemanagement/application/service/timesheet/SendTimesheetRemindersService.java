package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ProcessReminderBatchUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class SendTimesheetRemindersService implements SendTimesheetRemindersUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendTimesheetRemindersService.class);

    private final ProcessReminderBatchUseCase processReminderBatchUseCase;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final Clock clock;

    public SendTimesheetRemindersService(
            ProcessReminderBatchUseCase processReminderBatchUseCase,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort,
            Clock clock) {
        this.processReminderBatchUseCase = processReminderBatchUseCase;
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadHolidaysPort = loadHolidaysPort;
        this.clock = clock;
    }

    @Override
    public void sendReminders() {
        LocalDate today = LocalDate.now(clock);
        CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<LocalDate> holidays = loadHolidaysPort.getHolidayDatesBetween(monday, sunday);

        LocalDate lastWorkingDate = null;
        for (LocalDate d = sunday; !d.isBefore(monday); d = d.minusDays(1)) {
            if (calendar.isWorkingDay(d.getDayOfWeek()) && !holidays.contains(d)) {
                lastWorkingDate = d;
                break;
            }
        }

        if (lastWorkingDate == null || !today.equals(lastWorkingDate)) {
            return;
        }

        final int BATCH_SIZE = 500;
        int totalProcessed = 0;

        while (true) {
            try {
                int processed = processReminderBatchUseCase.processBatch(today, BATCH_SIZE);
                if (processed == 0) {
                    break;
                }
                totalProcessed += processed;
            } catch (Exception e) {
                log.error("Lỗi/Rollback khi xử lý batch nhắc nhở. Dừng tiến trình cron để tránh lặp vô hạn.", e);
                break;
            }
        }

        if (totalProcessed > 0) {
            log.info("Hoàn tất duyệt tổng cộng {} timesheets", totalProcessed);
        }
    }
}
