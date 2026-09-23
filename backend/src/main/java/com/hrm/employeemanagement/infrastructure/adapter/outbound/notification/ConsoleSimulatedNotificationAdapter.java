package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.user.UserId;

@Component
public class ConsoleSimulatedNotificationAdapter implements SimulatedNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSimulatedNotificationAdapter.class);
    private final SaveNotificationPort saveNotificationPort;

    public ConsoleSimulatedNotificationAdapter(
            SaveNotificationPort saveNotificationPort
    ) {
        this.saveNotificationPort = saveNotificationPort;
    }

    @Override
    public void sendScheduleConflictWarningNotification(
            Long recipientUserId,
            Long targetId,
            String recipientEmail,
            String recipientName,
            String employeeName,
            String conflictSummary,
            String details
    ) {
        if (recipientUserId == null || targetId == null) {
            log.warn("Bỏ qua cảnh báo xung đột do thiếu recipientUserId hoặc targetId");
            return;
        }
        UserId userId = new UserId(recipientUserId);
        String title = "Cảnh báo xung đột lịch nhân sự [" + employeeName + "]";
        saveNotificationPort.save(Notification.create(
                userId, null, NotificationType.SCHEDULE_CONFLICT,
                "SCHEDULE_CONFLICT", targetId, title,
                conflictSummary + (details == null || details.isBlank() ? "" : " - " + details)));
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
