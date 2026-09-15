package com.hrm.employeemanagement.infrastructure.transaction.timesheet;

import com.hrm.employeemanagement.application.port.inbound.timesheet.SendTimesheetRemindersUseCase;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSendTimesheetRemindersUseCase implements SendTimesheetRemindersUseCase {

    private final SendTimesheetRemindersUseCase delegate;

    public TransactionalSendTimesheetRemindersUseCase(SendTimesheetRemindersUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void sendReminders() {
        delegate.sendReminders();
    }
}
