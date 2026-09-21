package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dto;

import java.time.LocalTime;

import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;

public record UpdateNotificationPreferenceRequest(
        Boolean inAppEnabled,
        Boolean emailEnabled,
        String taskAssignedChannel,
        String taskDueReminderChannel,
        String taskCommentChannel,
        String timesheetReminderChannel,
        String allocationChangedChannel,
        String scheduleConflictChannel,
        String frequency,
        Integer taskDueReminderDays,
        Boolean quietHoursEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
    public UpdateNotificationPreferenceCommand toCommand() {
        return new UpdateNotificationPreferenceCommand(
                inAppEnabled,
                emailEnabled,
                parseChannel(taskAssignedChannel),
                parseChannel(taskDueReminderChannel),
                parseChannel(taskCommentChannel),
                parseChannel(timesheetReminderChannel),
                parseChannel(allocationChangedChannel),
                parseChannel(scheduleConflictChannel),
                parseFrequency(frequency),
                taskDueReminderDays,
                quietHoursEnabled,
                quietHoursStart,
                quietHoursEnd
        );
    }

    private NotificationDeliveryChannel parseChannel(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return NotificationDeliveryChannel.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Kênh thông báo không hợp lệ: " + val, e);
        }
    }

    private NotificationFrequency parseFrequency(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return NotificationFrequency.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tần suất thông báo không hợp lệ: " + val, e);
        }
    }
}
