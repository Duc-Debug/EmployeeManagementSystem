package com.hrm.employeemanagement.application.dto.notification;

import java.time.LocalTime;

import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;

public record UpdateNotificationPreferenceCommand(
        boolean inAppEnabled,
        boolean emailEnabled,
        NotificationDeliveryChannel taskAssignedChannel,
        NotificationDeliveryChannel taskDueReminderChannel,
        NotificationDeliveryChannel taskCommentChannel,
        NotificationDeliveryChannel timesheetReminderChannel,
        NotificationDeliveryChannel allocationChangedChannel,
        NotificationDeliveryChannel scheduleConflictChannel,
        NotificationFrequency frequency,
        int taskDueReminderDays,
        boolean quietHoursEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}
