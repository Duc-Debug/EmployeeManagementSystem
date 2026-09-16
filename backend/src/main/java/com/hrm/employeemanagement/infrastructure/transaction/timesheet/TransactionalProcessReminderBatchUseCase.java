package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.ProcessReminderBatchUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ReminderBatchResult;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

public class TransactionalProcessReminderBatchUseCase implements ProcessReminderBatchUseCase {

    private final ProcessReminderBatchUseCase delegate;

    public TransactionalProcessReminderBatchUseCase(ProcessReminderBatchUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ReminderBatchResult processBatch(LocalDate today, int batchSize) {
        return delegate.processBatch(today, batchSize);
    }
}
