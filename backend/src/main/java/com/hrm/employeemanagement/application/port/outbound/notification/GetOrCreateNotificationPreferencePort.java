package com.hrm.employeemanagement.application.port.outbound.notification;

import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.user.UserId;

public interface GetOrCreateNotificationPreferencePort {
    NotificationPreference getOrCreate(UserId userId);
}
