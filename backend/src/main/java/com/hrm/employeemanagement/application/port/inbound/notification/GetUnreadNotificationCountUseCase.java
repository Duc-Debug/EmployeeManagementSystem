package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.UnreadNotificationCountResult;

public interface GetUnreadNotificationCountUseCase {
    UnreadNotificationCountResult getUnreadCount(Long currentUserId);
}
