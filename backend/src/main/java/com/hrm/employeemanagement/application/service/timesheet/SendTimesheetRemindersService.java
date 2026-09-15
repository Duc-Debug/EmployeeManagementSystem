package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SendTimesheetRemindersService implements SendTimesheetRemindersUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendTimesheetRemindersService.class);

    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;
    private final SaveTimesheetHistoryPort saveTimesheetHistoryPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final Clock clock;

    public SendTimesheetRemindersService(
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            SaveTimesheetHistoryPort saveTimesheetHistoryPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort,
            Clock clock) {
        this.loadTimesheetPort = loadTimesheetPort;
        this.saveTimesheetPort = saveTimesheetPort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveNotificationPort = saveNotificationPort;
        this.saveTimesheetHistoryPort = saveTimesheetHistoryPort;
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
            List<Timesheet> draftTimesheets = loadTimesheetPort.findDraftTimesheetsForReminderUpTo(today, BATCH_SIZE);
            if (draftTimesheets.isEmpty()) {
                break;
            }

            List<EmployeeId> employeeIds = draftTimesheets.stream()
                    .map(Timesheet::getEmployeeId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<EmployeeId, Employee> employeeMap = loadEmployeePort.findAllByIdIn(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId, Function.identity()));

            List<Notification> notificationsToSave = new ArrayList<>();
            List<Timesheet> timesheetsToSave = new ArrayList<>();
            List<TimesheetHistory> historiesToSave = new ArrayList<>();

            for (Timesheet timesheet : draftTimesheets) {
                try {
                    Employee employee = employeeMap.get(timesheet.getEmployeeId());
                    if (employee != null && employee.getUserId() != null) {
                        String title = "Nhắc nộp bảng chấm công";
                        String content = "Bạn chưa nộp bảng chấm công cho tuần bắt đầu từ " + timesheet.getWeekStartDate() + ". Vui lòng nộp sớm.";

                        Notification notification = Notification.create(
                                employee.getUserId(),
                                null, // System sender
                                NotificationType.TIMESHEET_REMINDER,
                                "TIMESHEET",
                                timesheet.getId().value(),
                                title,
                                content
                        );
                        notificationsToSave.add(notification);

                        timesheet.setRemindedAt(LocalDateTime.now(clock));
                        timesheetsToSave.add(timesheet);

                        TimesheetHistory history = TimesheetHistory.create(
                                timesheet.getId(),
                                "REMINDER_SENT",
                                null, // System action
                                "System sent a reminder to submit timesheet"
                        );
                        historiesToSave.add(history);
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi tạo nhắc nhở cho timesheet ID: {}", timesheet.getIdValue(), e);
                }
            }

            if (!notificationsToSave.isEmpty()) {
                saveNotificationPort.saveAll(notificationsToSave);
                saveTimesheetPort.saveAll(timesheetsToSave);
                saveTimesheetHistoryPort.saveAll(historiesToSave);
                totalProcessed += notificationsToSave.size();
                log.info("Đã gửi {} nhắc nhở nộp bảng chấm công trong batch này", notificationsToSave.size());
            }
        }

        if (totalProcessed > 0) {
            log.info("Hoàn tất gửi tổng cộng {} nhắc nhở", totalProcessed);
        }
    }
}
