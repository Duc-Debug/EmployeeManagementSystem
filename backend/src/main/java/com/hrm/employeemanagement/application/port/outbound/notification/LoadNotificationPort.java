package com.hrm.employeemanagement.application.port.outbound.notification;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationId;
import com.hrm.employeemanagement.domain.user.UserId;

public interface LoadNotificationPort {
    Optional<Notification> findById(NotificationId id);

    List<Notification> findAllByRecipientId(UserId recipientId);
}

