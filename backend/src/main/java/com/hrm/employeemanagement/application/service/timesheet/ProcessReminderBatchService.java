package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ProcessReminderBatchUseCase;
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
    public int processBatch(LocalDate today, int batchSize) {
        List<Timesheet> draftTimesheets = loadTimesheetPort.findDraftTimesheetsForReminderUpTo(today, batchSize);
        if (draftTimesheets.isEmpty()) {
            return 0;
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

        LocalDateTime now = LocalDateTime.now(clock);

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

                    // Only mark the timesheet after its reminder and audit history
                    // have both been created. Otherwise it remains eligible for retry.
                    timesheet.setRemindedAt(now);
                    timesheetsToSave.add(timesheet);
                } else {
                    log.warn("Không thể gửi nhắc nhở cho timesheet ID: {} vì employee hoặc userId bị null", timesheet.getIdValue());
                }
            } catch (Exception e) {
                log.error("Lỗi khi tạo nhắc nhở cho timesheet ID: {}", timesheet.getIdValue(), e);
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

        log.info("Đã xử lý batch {} timesheets, gửi {} thông báo", timesheetsToSave.size(), notificationsToSave.size());
        return timesheetsToSave.size();
    }
}
