package com.hrm.employeemanagement.application.dto.notification;

import java.time.LocalTime;

import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.domain.notification.NotificationPreference;

public record NotificationPreferenceResult(
        Long id,
        Long userId,
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
        LocalTime quietHoursEnd,
        Long version
) {
    public static NotificationPreferenceResult fromDomain(NotificationPreference domain) {
        if (domain == null) {
            return null;
        }
        return new NotificationPreferenceResult(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getUserId() != null ? domain.getUserId().value() : null,
                domain.isInAppEnabled(),
                domain.isEmailEnabled(),
                domain.getTaskAssignedChannel(),
                domain.getTaskDueReminderChannel(),
                domain.getTaskCommentChannel(),
                domain.getTimesheetReminderChannel(),
                domain.getAllocationChangedChannel(),
                domain.getScheduleConflictChannel(),
                domain.getFrequency(),
                domain.getTaskDueReminderDays(),
                domain.getQuietHours() != null && domain.getQuietHours().enabled(),
                domain.getQuietHours() != null ? domain.getQuietHours().startTime() : null,
                domain.getQuietHours() != null ? domain.getQuietHours().endTime() : null,
                domain.getVersion()
        );
    }
}
