package com.hrm.employeemanagement.application.port.inbound.notification;

import java.util.List;

import com.hrm.employeemanagement.application.dto.notification.NotificationResult;

public interface GetNotificationsUseCase {
    List<NotificationResult> execute(Long recipientUserId);
}

