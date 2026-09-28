package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.UpdateNotificationDedupConfigCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.GetNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.TriggerManualOverloadScanUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.UpdateNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.service.notification.dedup.NotificationDedupConfigService;

/**
 * Keeps configuration persistence and its history entry in one transaction.
 */
public class TransactionalNotificationDedupConfigServiceDecorator implements
        GetNotificationDedupConfigUseCase,
        UpdateNotificationDedupConfigUseCase,
        TriggerManualOverloadScanUseCase {

    private final NotificationDedupConfigService delegate;

    public TransactionalNotificationDedupConfigServiceDecorator(NotificationDedupConfigService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "NotificationDedupConfigService must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDedupConfigResult getConfig() {
        return delegate.getConfig();
    }

    @Override
    @Transactional
    public NotificationDedupConfigResult updateConfig(UpdateNotificationDedupConfigCommand command, Long actorUserId) {
        return delegate.updateConfig(command, actorUserId);
    }

    @Override
    public OverloadScanResult triggerManualScan(Integer year, Integer weekNumber) {
        return delegate.triggerManualScan(year, weekNumber);
    }
}
