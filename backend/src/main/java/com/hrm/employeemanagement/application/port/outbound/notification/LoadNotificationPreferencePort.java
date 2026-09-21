package com.hrm.employeemanagement.application.port.outbound.notification;

import java.util.Optional;

import com.hrm.employeemanagement.domain.notification.NotificationPreference;
import com.hrm.employeemanagement.domain.user.UserId;

public interface LoadNotificationPreferencePort {
    Optional<NotificationPreference> findByUserId(UserId userId);
}
