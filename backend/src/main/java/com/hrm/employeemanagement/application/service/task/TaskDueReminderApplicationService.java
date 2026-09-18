package com.hrm.employeemanagement.application.service.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;
import com.hrm.employeemanagement.application.port.outbound.task.CheckTaskDueReminderSentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.notification.TaskDueReminderPolicy;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;

/**
 * Application Service điều phối rà soát công việc sắp đến hạn (NCL-11-CN-004).
 * Tuân thủ nghiêm ngặt quy tắc chống gửi trùng thông báo QTN-19.
 * Pure Java, không sử dụng Spring annotations trực tiếp theo backend-guideline.
 */
public class TaskDueReminderApplicationService implements ScanAndSendTaskDueRemindersUseCase {

    private final LoadTaskDueReminderPort loadTaskDueReminderPort;
    private final CheckTaskDueReminderSentPort checkTaskDueReminderSentPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;
    private final CreateNotificationEventUseCase createNotificationEventUseCase;
    private final SaveAuditLogPort saveAuditLogPort;

    public TaskDueReminderApplicationService(
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            CheckTaskDueReminderSentPort checkTaskDueReminderSentPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            CreateNotificationEventUseCase createNotificationEventUseCase,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.loadTaskDueReminderPort = Objects.requireNonNull(loadTaskDueReminderPort, "loadTaskDueReminderPort must not be null");
        this.checkTaskDueReminderSentPort = Objects.requireNonNull(checkTaskDueReminderSentPort, "checkTaskDueReminderSentPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.saveNotificationPort = saveNotificationPort;
        this.createNotificationEventUseCase = createNotificationEventUseCase;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public TaskDueReminderScanResult execute(LocalDate scanDate) {
        LocalDate effectiveScanDate = scanDate != null ? scanDate : LocalDate.now();
        LocalDate toDate = effectiveScanDate.plusDays(TaskDueReminderPolicy.DEFAULT_DUE_SOON_DAYS);

        List<Task> candidateTasks = loadTaskDueReminderPort.findTasksDueBetween(effectiveScanDate, toDate);

        int totalScanned = 0;
        int sentCount = 0;
        int skippedDuplicateCount = 0;
        int skippedCompletedCount = 0;
        List<Long> notifiedTaskIds = new ArrayList<>();

        for (Task task : candidateTasks) {
            totalScanned++;

            // 1. Kiểm tra trạng thái hoàn thành / hủy -> Bỏ qua (TC-02)
            if (!TaskDueReminderPolicy.isEligibleStatus(task.getStatus())) {
                skippedCompletedCount++;
                continue;
            }

            LocalDate dueDate = task.getDueDate() != null ? task.getDueDate() : task.getPlannedEndDate();
            if (dueDate == null || !TaskDueReminderPolicy.isDueWithinDays(dueDate, effectiveScanDate, TaskDueReminderPolicy.DEFAULT_DUE_SOON_DAYS)) {
                continue;
            }

            // 2. Kiểm tra người phụ trách
            if (task.getAssigneeId() == null) {
                continue;
            }

            Optional<Employee> employeeOpt = loadEmployeePort.findById(task.getAssigneeId());
            if (employeeOpt.isEmpty() || employeeOpt.get().getUserId() == null) {
                continue;
            }

            UserId recipientUserId = employeeOpt.get().getUserId();

            // 3. Quy tắc QTN-19: Kiểm tra thông báo đã từng gửi cho sự kiện này hay chưa
            if (checkTaskDueReminderSentPort.hasReminderBeenSent(recipientUserId, task.getIdValue(), dueDate)) {
                skippedDuplicateCount++;
                continue;
            }

            // 4. Tạo thông báo nhắc việc
            long daysRemaining = TaskDueReminderPolicy.calculateDaysRemaining(dueDate, effectiveScanDate);
            String directUrl = TaskDueReminderPolicy.buildDirectTaskUrl(task.getProjectIdValue(), task.getIdValue());
            String title = TaskDueReminderPolicy.buildNotificationTitle(task.getName());
            String content = TaskDueReminderPolicy.buildNotificationContent(task.getName(), dueDate, daysRemaining, directUrl);

            // Lưu vào hệ thống thông báo mới (Notification Center nếu có)
            if (createNotificationEventUseCase != null) {
                String sourceEventKey = TaskDueReminderPolicy.buildSourceEventKey(task.getIdValue(), dueDate);
                NotificationLevel level = TaskDueReminderPolicy.determineNotificationLevel(daysRemaining);

                CreateNotificationEventCommand eventCommand = new CreateNotificationEventCommand(
                        "TASK_DUE_REMINDER",
                        level,
                        title,
                        content,
                        "TASK",
                        String.valueOf(task.getIdValue()),
                        sourceEventKey,
                        List.of(recipientUserId.value())
                );
                createNotificationEventUseCase.execute(eventCommand);
            }

            // Lưu vào bảng notifications truyền thống (hỗ trợ NotificationPopover & các API hiện tại)
            if (saveNotificationPort != null) {
                Notification notification = Notification.create(
                        recipientUserId,
                        null,
                        NotificationType.TASK_DUE_REMINDER,
                        "TASK",
                        task.getIdValue(),
                        title,
                        content
                );
                saveNotificationPort.save(notification);
            }

            sentCount++;
            notifiedTaskIds.add(task.getIdValue());
        }

        // 5. Ghi nhận nhật ký kiểm toán thao tác (TC-04)
        if (saveAuditLogPort != null) {
            String logDetail = String.format(
                    "scanDate=%s;totalScanned=%d;sentCount=%d;skippedDuplicateCount=%d;skippedCompletedCount=%d;notifiedTaskIds=%s",
                    effectiveScanDate, totalScanned, sentCount, skippedDuplicateCount, skippedCompletedCount, notifiedTaskIds
            );
            saveAuditLogPort.save(
                    AuditLog.createChange(
                            null, // SYSTEM actor
                            "SCAN_TASK_DUE_REMINDERS",
                            "tasks",
                            null,
                            null,
                            logDetail
                    )
            );
        }

        return new TaskDueReminderScanResult(
                effectiveScanDate,
                totalScanned,
                sentCount,
                skippedDuplicateCount,
                skippedCompletedCount,
                notifiedTaskIds
        );
    }
}
