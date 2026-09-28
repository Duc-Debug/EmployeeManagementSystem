package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TimesheetReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TimesheetReminderJob.class);

    private final SendTimesheetRemindersUseCase sendTimesheetRemindersUseCase;

    public TimesheetReminderJob(SendTimesheetRemindersUseCase sendTimesheetRemindersUseCase) {
        this.sendTimesheetRemindersUseCase = sendTimesheetRemindersUseCase;
    }

    @Scheduled(cron = "0 0 23 * * *")
    public void execute() {
        log.info("Starting TimesheetReminderJob...");
        try {
            sendTimesheetRemindersUseCase.sendReminders();
            log.info("TimesheetReminderJob completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred while running TimesheetReminderJob", e);
        }
    }
}
