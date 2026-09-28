package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.GetNotificationCenterQuery;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterPageResult;

public interface GetNotificationCenterUseCase {
    NotificationCenterPageResult getNotifications(Long currentUserId, GetNotificationCenterQuery query);
}
