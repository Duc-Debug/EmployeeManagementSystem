package com.hrm.employeemanagement.application.port.outbound.notification;

import com.hrm.employeemanagement.domain.notification.NotificationPreference;

public interface SaveNotificationPreferencePort {
    NotificationPreference save(NotificationPreference preference);
}
