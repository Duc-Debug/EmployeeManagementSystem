package com.hrm.employeemanagement.application.port.outbound.notification.dedup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupRecord;

public interface NotificationDedupRepositoryPort {
    Optional<NotificationDedupRecord> findActiveByDedupKey(String dedupKey);
    NotificationDedupRecord save(NotificationDedupRecord record);
    List<NotificationDedupRecord> findActiveByTargetAndWeek(String targetEntityType, String targetEntityId, String yearWeek);
    void resolveActiveRecords(String targetEntityType, String targetEntityId, String yearWeek, LocalDateTime resolvedAt);
}
