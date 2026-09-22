package com.hrm.employeemanagement.application.port.inbound.notification.dedup;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.UpdateNotificationDedupConfigCommand;

public interface UpdateNotificationDedupConfigUseCase {
    NotificationDedupConfigResult updateConfig(UpdateNotificationDedupConfigCommand command, Long actorUserId);
}
