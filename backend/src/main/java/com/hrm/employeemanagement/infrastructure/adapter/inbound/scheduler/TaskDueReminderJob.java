package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import java.time.LocalDate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;

/**
 * Tác vụ nền rà soát định kỳ hằng ngày các công việc sắp đến hạn trong 3 ngày tới (NCL-11-CN-004).
 * Chạy tự động lúc 08:00 sáng mỗi ngày.
 * Tuân thủ nghiêm ngặt quy tắc chống gửi trùng QTN-19.
 */
@Component
public class TaskDueReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TaskDueReminderJob.class);

    private final ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase;

    public TaskDueReminderJob(ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase) {
        this.scanAndSendTaskDueRemindersUseCase = Objects.requireNonNull(scanAndSendTaskDueRemindersUseCase, "scanAndSendTaskDueRemindersUseCase must not be null");
    }

    @Scheduled(cron = "${app.scheduling.task-due-reminder.cron:0 0 8 * * *}")
    public void execute() {
        log.info("Bắt đầu tác vụ nền TaskDueReminderJob...");
        try {
            LocalDate today = LocalDate.now();
            TaskDueReminderScanResult result = scanAndSendTaskDueRemindersUseCase.execute(today);
            log.info("TaskDueReminderJob hoàn tất. Quét: {}, Đã gửi: {}, Bỏ qua trùng (QTN-19): {}, Bỏ qua hoàn thành: {}",
                    result.totalScanned(), result.sentCount(), result.skippedDuplicateCount(), result.skippedCompletedCount());
        } catch (Exception e) {
            log.error("Lỗi khi thực thi tác vụ nền TaskDueReminderJob", e);
        }
    }
}
