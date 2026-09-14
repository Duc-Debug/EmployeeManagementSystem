package com.hrm.employeemanagement.application.port.outbound.notification;

public interface SimulatedNotificationPort {
    void sendScheduleConflictWarningNotification(
            String recipientEmail,
            String recipientName,
            String employeeName,
            String conflictSummary,
            String details
    );
}
