package com.hrm.employeemanagement.application.port.inbound.notification.dedup;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;

public interface GetNotificationDedupConfigUseCase {
    NotificationDedupConfigResult getConfig();
}
