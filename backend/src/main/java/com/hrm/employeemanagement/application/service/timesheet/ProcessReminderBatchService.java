package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ProcessReminderBatchUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ReminderBatchResult;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
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

public class ProcessReminderBatchService implements ProcessReminderBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessReminderBatchService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;
    private final SaveTimesheetHistoryPort saveTimesheetHistoryPort;
    private final Clock clock;

    public ProcessReminderBatchService(
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            SaveTimesheetHistoryPort saveTimesheetHistoryPort,
            Clock clock) {
        this.loadTimesheetPort = loadTimesheetPort;
        this.saveTimesheetPort = saveTimesheetPort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveNotificationPort = saveNotificationPort;
        this.saveTimesheetHistoryPort = saveTimesheetHistoryPort;
        this.clock = clock;
    }

    @Override
    public ReminderBatchResult processBatch(LocalDate today, int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Timesheet> draftTimesheets = loadTimesheetPort.findDraftTimesheetsForReminderUpTo(today, now, batchSize);
        if (draftTimesheets.isEmpty()) {
            return ReminderBatchResult.empty();
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

        int sent = 0;
        int retryScheduled = 0;
        int permanentlyFailed = 0;

        for (Timesheet timesheet : draftTimesheets) {
            try {
                Employee employee = employeeMap.get(timesheet.getEmployeeId());
                if (employee != null && employee.getUserId() != null) {
                    String title = "Nhắc nộp bảng chấm công";
                    String content = "Bạn chưa nộp bảng chấm công cho tuần bắt đầu từ " + timesheet.getWeekStartDate() + ". Vui lòng nộp sớm.";

                    Notification notification = Notification.create(
                            employee.getUserId(),
                            null,
                            NotificationType.TIMESHEET_REMINDER,
                            "TIMESHEET",
                            timesheet.getId().value(),
                            title,
                            content
                    );
                    notificationsToSave.add(notification);

                    TimesheetHistory history = TimesheetHistory.create(
                            timesheet.getId(),
                            "REMINDER_SENT",
                            null,
                            "System sent a reminder to submit timesheet"
                    );
                    historiesToSave.add(history);

                    timesheet.markReminderSent(now);
                    timesheetsToSave.add(timesheet);
                    sent++;
                } else {
                    String error = "Employee or linked user is missing";
                    log.warn("Không thể gửi nhắc nhở cho timesheet ID: {} vì {}", timesheet.getIdValue(), error);
                    timesheet.markReminderFailed(error);
                    timesheetsToSave.add(timesheet);
                    permanentlyFailed++;
                }
            } catch (Exception e) {
                log.error("Lỗi khi tạo nhắc nhở cho timesheet ID: {}", timesheet.getIdValue(), e);
                String error = messageFor(e);
                if (timesheet.getReminderAttemptCount() + 1 >= MAX_RETRY_ATTEMPTS) {
                    timesheet.markReminderFailed("Retry limit reached: " + error);
                    permanentlyFailed++;
                } else {
                    timesheet.scheduleReminderRetry(now.plusDays(1), error);
                    retryScheduled++;
                }
                timesheetsToSave.add(timesheet);
            }
        }

        if (!notificationsToSave.isEmpty()) {
            saveNotificationPort.saveAll(notificationsToSave);
        }
        if (!timesheetsToSave.isEmpty()) {
            saveTimesheetPort.saveAll(timesheetsToSave);
        }
        if (!historiesToSave.isEmpty()) {
            saveTimesheetHistoryPort.saveAll(historiesToSave);
        }

        log.info("Đã quét {} timesheets: sent={}, retry={}, failed={}", draftTimesheets.size(), sent, retryScheduled, permanentlyFailed);
        return new ReminderBatchResult(draftTimesheets.size(), sent, retryScheduled, permanentlyFailed);
    }

    private String messageFor(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
