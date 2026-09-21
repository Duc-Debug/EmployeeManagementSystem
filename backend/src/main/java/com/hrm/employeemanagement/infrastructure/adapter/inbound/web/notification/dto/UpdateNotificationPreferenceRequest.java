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
                inAppEnabled != null ? inAppEnabled : true,
                emailEnabled != null ? emailEnabled : true,
                parseChannel(taskAssignedChannel, NotificationDeliveryChannel.ALL),
                parseChannel(taskDueReminderChannel, NotificationDeliveryChannel.ALL),
                parseChannel(taskCommentChannel, NotificationDeliveryChannel.IN_APP_ONLY),
                parseChannel(timesheetReminderChannel, NotificationDeliveryChannel.ALL),
                parseChannel(allocationChangedChannel, NotificationDeliveryChannel.ALL),
                parseChannel(scheduleConflictChannel, NotificationDeliveryChannel.ALL),
                parseFrequency(frequency, NotificationFrequency.IMMEDIATE),
                taskDueReminderDays != null ? taskDueReminderDays : 3,
                quietHoursEnabled != null ? quietHoursEnabled : false,
                quietHoursStart,
                quietHoursEnd
        );
    }

    private NotificationDeliveryChannel parseChannel(String val, NotificationDeliveryChannel defaultChannel) {
        if (val == null || val.isBlank()) {
            return defaultChannel;
        }
        try {
            return NotificationDeliveryChannel.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultChannel;
        }
    }

    private NotificationFrequency parseFrequency(String val, NotificationFrequency defaultFreq) {
        if (val == null || val.isBlank()) {
            return defaultFreq;
        }
        try {
            return NotificationFrequency.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultFreq;
        }
    }
}
