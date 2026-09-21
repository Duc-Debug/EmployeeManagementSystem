package com.hrm.employeemanagement.application.port.outbound.notification.dedup;

import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfigHistory;

public interface NotificationDedupConfigRepositoryPort {
    NotificationDedupConfig loadConfig();
    NotificationDedupConfig saveConfig(NotificationDedupConfig config);
    void saveHistory(NotificationDedupConfigHistory history);
}
