package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final Clock clock;

    public SendTimesheetRemindersService(
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            SaveTimesheetHistoryPort saveTimesheetHistoryPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            Clock clock) {
        this.loadTimesheetPort = loadTimesheetPort;
        this.saveTimesheetPort = saveTimesheetPort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveNotificationPort = saveNotificationPort;
        this.saveTimesheetHistoryPort = saveTimesheetHistoryPort;
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.clock = clock;
    }

    @Override
    public void sendReminders() {
        LocalDate today = LocalDate.now(clock);
        CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();

        if (today.getDayOfWeek() != calendar.getLastWorkingDayOfWeek()) {
            return;
        }

        List<Timesheet> draftTimesheets = loadTimesheetPort.findDraftTimesheetsForReminderUpTo(today);
        if (draftTimesheets.isEmpty()) {
            return;
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

        try {
            if (!notificationsToSave.isEmpty()) {
                saveNotificationPort.saveAll(notificationsToSave);
                saveTimesheetPort.saveAll(timesheetsToSave);
                saveTimesheetHistoryPort.saveAll(historiesToSave);
                log.info("Đã gửi {} nhắc nhở nộp bảng chấm công", notificationsToSave.size());
            }
        } catch (Exception e) {
            log.error("Lỗi khi lưu batch thông báo nhắc nhở", e);
        }
    }
}
