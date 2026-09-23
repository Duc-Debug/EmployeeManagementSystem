package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;

public interface ResetNotificationPreferenceUseCase {
    NotificationPreferenceResult resetMyPreference(Long currentUserId);
}
