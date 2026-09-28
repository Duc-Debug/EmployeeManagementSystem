package com.hrm.employeemanagement.application.port.inbound.notification;

import com.hrm.employeemanagement.application.dto.notification.NotificationCenterItemResult;

public interface GetNotificationDetailUseCase {
    NotificationCenterItemResult getNotificationDetail(Long recipientId, Long currentUserId);
}
