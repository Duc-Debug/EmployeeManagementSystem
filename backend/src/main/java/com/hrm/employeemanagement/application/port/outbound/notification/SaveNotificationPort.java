package com.hrm.employeemanagement.application.port.outbound.notification;

import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.user.UserId;

public interface SaveNotificationPort {
    Notification save(Notification notification);

    void markAllAsRead(UserId recipientId);
}

