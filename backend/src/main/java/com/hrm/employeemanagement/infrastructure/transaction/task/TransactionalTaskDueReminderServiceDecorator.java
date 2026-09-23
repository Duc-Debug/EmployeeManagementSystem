package com.hrm.employeemanagement.infrastructure.transaction.task;

import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;

/**
 * Transaction boundary decorator cho các use cases Nhắc việc sắp đến hạn (NCL-11-CN-004).
 * Đảm bảo các use case chạy trong transaction phù hợp theo Hexagonal Architecture.
 */
public class TransactionalTaskDueReminderServiceDecorator
        implements ScanAndSendTaskDueRemindersUseCase, GetMyUpcomingDueTasksUseCase {

    private final ScanAndSendTaskDueRemindersUseCase scanAndSendDelegate;
    private final GetMyUpcomingDueTasksUseCase getUpcomingTasksDelegate;

    public TransactionalTaskDueReminderServiceDecorator(
            ScanAndSendTaskDueRemindersUseCase scanAndSendDelegate,
            GetMyUpcomingDueTasksUseCase getUpcomingTasksDelegate
    ) {
        this.scanAndSendDelegate = scanAndSendDelegate;
        this.getUpcomingTasksDelegate = getUpcomingTasksDelegate;
    }

    public TransactionalTaskDueReminderServiceDecorator(ScanAndSendTaskDueRemindersUseCase scanAndSendDelegate) {
        this(scanAndSendDelegate, null);
    }

    public TransactionalTaskDueReminderServiceDecorator(GetMyUpcomingDueTasksUseCase getUpcomingTasksDelegate) {
        this(null, getUpcomingTasksDelegate);
    }

    @Override
    @Transactional
    public TaskDueReminderScanResult execute(LocalDate scanDate) {
        if (scanAndSendDelegate == null) {
            throw new UnsupportedOperationException("ScanAndSendTaskDueRemindersUseCase delegate is not configured");
        }
        return scanAndSendDelegate.execute(scanDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingDueTaskResult> execute() {
        if (getUpcomingTasksDelegate == null) {
            throw new UnsupportedOperationException("GetMyUpcomingDueTasksUseCase delegate is not configured");
        }
        return getUpcomingTasksDelegate.execute();
    }
}