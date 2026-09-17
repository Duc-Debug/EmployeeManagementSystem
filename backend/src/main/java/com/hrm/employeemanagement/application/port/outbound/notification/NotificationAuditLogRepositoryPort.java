package com.hrm.employeemanagement.application.port.outbound.notification;

import java.util.List;

import com.hrm.employeemanagement.domain.notification.NotificationAuditLog;
import com.hrm.employeemanagement.domain.user.UserId;

public interface NotificationAuditLogRepositoryPort {
    NotificationAuditLog save(NotificationAuditLog auditLog);
    List<NotificationAuditLog> findByActorUserId(UserId actorUserId);
}
