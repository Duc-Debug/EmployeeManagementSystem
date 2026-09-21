package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;

public interface UpdateNotificationPreferenceUseCase {
    NotificationPreferenceResult updateMyPreference(Long currentUserId, UpdateNotificationPreferenceCommand command);
}
