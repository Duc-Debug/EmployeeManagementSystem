package com.hrm.employeemanagement.application.service.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPreferencePort;
import com.hrm.employeemanagement.application.port.outbound.task.CheckTaskDueReminderSentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.notification.TaskDueReminderPolicy;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service điều phối rà soát công việc sắp đến hạn (NCL-11-CN-004).
 * Tuân thủ nghiêm ngặt quy tắc chống gửi trùng thông báo QTN-19.
 * Tối ưu hóa hiệu năng bằng Batch Loading để xóa bỏ triệt để N+1 Query.
 * Pure Java, không sử dụng Spring annotations trực tiếp theo backend-guideline.
 */
public class TaskDueReminderApplicationService implements ScanAndSendTaskDueRemindersUseCase {

    private final LoadTaskDueReminderPort loadTaskDueReminderPort;
    private final CheckTaskDueReminderSentPort checkTaskDueReminderSentPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;
    private final CreateNotificationEventUseCase createNotificationEventUseCase;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadNotificationPreferencePort loadNotificationPreferencePort;
    private final Clock clock;

    public TaskDueReminderApplicationService(
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            CheckTaskDueReminderSentPort checkTaskDueReminderSentPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            CreateNotificationEventUseCase createNotificationEventUseCase,
            SaveAuditLogPort saveAuditLogPort,
            LoadNotificationPreferencePort loadNotificationPreferencePort,
            Clock clock
    ) {
        this.loadTaskDueReminderPort = Objects.requireNonNull(loadTaskDueReminderPort, "loadTaskDueReminderPort must not be null");
        this.checkTaskDueReminderSentPort = Objects.requireNonNull(checkTaskDueReminderSentPort, "checkTaskDueReminderSentPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.saveNotificationPort = saveNotificationPort;
        this.createNotificationEventUseCase = createNotificationEventUseCase;
        this.saveAuditLogPort = saveAuditLogPort;
        this.loadNotificationPreferencePort = loadNotificationPreferencePort;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public TaskDueReminderApplicationService(
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            CheckTaskDueReminderSentPort checkTaskDueReminderSentPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            CreateNotificationEventUseCase createNotificationEventUseCase,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this(loadTaskDueReminderPort, checkTaskDueReminderSentPort, loadEmployeePort, saveNotificationPort,
                createNotificationEventUseCase, saveAuditLogPort, null, Clock.systemDefaultZone());
    }

    @Override
    public TaskDueReminderScanResult execute(LocalDate scanDate) {
        LocalDate effectiveScanDate = scanDate != null ? scanDate : LocalDate.now(clock);
        LocalDate toDate = effectiveScanDate.plusDays(
                com.hrm.employeemanagement.domain.notification.NotificationPreference.MAX_TASK_DUE_REMINDER_DAYS);

        List<Task> candidateTasks = loadTaskDueReminderPort.findTasksDueBetween(effectiveScanDate, toDate);

        // Tối ưu hóa N+1 query: Nạp hàng loạt (Batch load) danh sách nhân sự phụ trách bằng 1 query duy nhất
        List<EmployeeId> assigneeIds = candidateTasks.stream()
                .map(Task::getAssigneeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<EmployeeId, Employee> employeeMap = assigneeIds.isEmpty()
                ? Map.of()
                : loadEmployeePort.findAllByIdIn(assigneeIds).stream()
                        .filter(e -> e.getId() != null)
                        .collect(Collectors.toMap(Employee::getId, e -> e, (first, ignored) -> first));

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
            if (dueDate == null) {
                continue;
            }

            // 2. Kiểm tra người phụ trách từ preloaded Map (O(1))
            if (task.getAssigneeId() == null) {
                continue;
            }

            Employee employee = employeeMap.get(task.getAssigneeId());
            if (employee == null || employee.getUserId() == null) {
                continue;
            }

            UserId recipientUserId = employee.getUserId();
            int reminderDays = loadNotificationPreferencePort == null
                    ? TaskDueReminderPolicy.DEFAULT_DUE_SOON_DAYS
                    : loadNotificationPreferencePort.findByUserId(recipientUserId)
                            .map(com.hrm.employeemanagement.domain.notification.NotificationPreference::getTaskDueReminderDays)
                            .orElse(TaskDueReminderPolicy.DEFAULT_DUE_SOON_DAYS);
            if (!TaskDueReminderPolicy.isDueWithinDays(dueDate, effectiveScanDate, reminderDays)) {
                continue;
            }

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

            // Lưu vào bảng notifications truyền thống trước để đảm bảo tính nhất quán (Atomicity)
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

            // Lưu vào hệ thống thông báo mới (Notification Center nếu có)
            if (createNotificationEventUseCase != null) {
                String sourceEventKey = TaskDueReminderPolicy.buildSourceEventKey(task.getIdValue(), recipientUserId.value(), dueDate);
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

            sentCount++;
            notifiedTaskIds.add(task.getIdValue());
        }

        // 5. Ghi nhận nhật ký kiểm toán thao tác (TC-04) - Cắt chuỗi an toàn tránh DataTruncation
        if (saveAuditLogPort != null) {
            String logDetail = String.format(
                    "scanDate=%s;totalScanned=%d;sentCount=%d;skippedDuplicateCount=%d;skippedCompletedCount=%d;notifiedTaskIds=%s",
                    effectiveScanDate, totalScanned, sentCount, skippedDuplicateCount, skippedCompletedCount, notifiedTaskIds
            );
            if (logDetail.length() > 1900) {
                logDetail = logDetail.substring(0, 1900) + "...[TRUNCATED]";
            }
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
