package com.hrm.employeemanagement.application.port.outbound.notification;

import com.hrm.employeemanagement.domain.notification.NotificationDeliveryDecision;
import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.user.UserId;

/** Schedules the email projection of a canonical notification event. */
public interface NotificationEmailDeliveryPort {
    void schedule(NotificationEvent sourceEvent, UserId recipientUserId, NotificationDeliveryDecision decision);
}
