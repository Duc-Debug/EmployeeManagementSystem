package com.hrm.employeemanagement.application.port.outbound.notification;

public interface SimulatedNotificationPort {
    void sendScheduleConflictWarningNotification(
            Long recipientUserId,
            Long targetId,
            String recipientEmail,
            String recipientName,
            String employeeName,
            String conflictSummary,
            String details
    );
    void sendReplacementSuggestionNotification(
            String recipientEmail,
            String recipientName,
            String originalEmployeeName,
            String replacementEmployeeName,
            String proposalDetails
    );
}
