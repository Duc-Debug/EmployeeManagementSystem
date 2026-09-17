package com.hrm.employeemanagement.application.port.outbound.notification;

import java.time.LocalDateTime;
import java.util.Optional;

import com.hrm.employeemanagement.domain.notification.NotificationEvent;
import com.hrm.employeemanagement.domain.notification.NotificationEventId;

public interface NotificationEventRepositoryPort {
    NotificationEvent save(NotificationEvent event);
    NotificationEvent getOrCreate(NotificationEvent event);
    Optional<NotificationEvent> findById(NotificationEventId id);
    java.util.List<NotificationEvent> findAllByIds(java.util.List<NotificationEventId> ids);
    Optional<NotificationEvent> findBySourceEventKey(String sourceEventKey);
    long purgeOrphanEventsOlderThan(LocalDateTime cutoff);
}
