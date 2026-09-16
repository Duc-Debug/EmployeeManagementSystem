package com.hrm.employeemanagement.application.port.inbound.timesheet;

import java.time.LocalDate;

public interface ProcessReminderBatchUseCase {
    int processBatch(LocalDate today, int batchSize);
}
