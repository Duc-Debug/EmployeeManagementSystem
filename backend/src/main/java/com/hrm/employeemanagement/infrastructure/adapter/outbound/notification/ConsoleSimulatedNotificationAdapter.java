package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;

@Component
public class ConsoleSimulatedNotificationAdapter implements SimulatedNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSimulatedNotificationAdapter.class);

    @Override
    public void sendScheduleConflictWarningNotification(
            String recipientEmail,
            String recipientName,
            String employeeName,
            String conflictSummary,
            String details
    ) {
        log.info("[SIMULATED NOTIFICATION] Sent to: {} ({}) | Subject: Cảnh báo xung đột lịch nhân sự [{}] | Summary: {} | Details: {}",
                recipientName, recipientEmail, employeeName, conflictSummary, details);
    }

    @Override
    public void sendReplacementSuggestionNotification(
            String recipientEmail,
            String recipientName,
            String originalEmployeeName,
            String replacementEmployeeName,
            String proposalDetails
    ) {
        log.info("[SIMULATED NOTIFICATION] Sent to: {} ({}) | Subject: Đề xuất nhân sự thay thế cho [{}] -> Thay thế bằng [{}] | Details: {}",
                recipientName, recipientEmail, originalEmployeeName, replacementEmployeeName, proposalDetails);
    }
}
