package com.hrm.employeemanagement.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.LoadWeeklyOverloadCandidatesPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatcherPort;
import com.hrm.employeemanagement.application.service.notification.dedup.NotificationDedupConfigService;
import com.hrm.employeemanagement.application.service.notification.dedup.OverloadAlertScanService;
import com.hrm.employeemanagement.infrastructure.transaction.notification.TransactionalNotificationDedupConfigServiceDecorator;

@Configuration
public class NotificationDedupUseCaseConfig {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OverloadAlertScanService overloadAlertScanService(
            NotificationDedupConfigRepositoryPort configRepositoryPort,
            NotificationDedupRepositoryPort dedupRepositoryPort,
            LoadWeeklyOverloadCandidatesPort loadCandidatesPort,
            OverloadAlertDispatcherPort overloadAlertDispatcherPort,
            Clock clock
    ) {
        return new OverloadAlertScanService(
                configRepositoryPort,
                dedupRepositoryPort,
                loadCandidatesPort,
                overloadAlertDispatcherPort,
                clock
        );
    }

    @Bean
    public NotificationDedupConfigService notificationDedupConfigService(
            NotificationDedupConfigRepositoryPort configRepositoryPort,
            GetAuthenticatedUserPort authenticatedUserPort,
            PermissionQueryPort permissionQueryPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase
    ) {
        return new NotificationDedupConfigService(
                configRepositoryPort,
                authenticatedUserPort,
                permissionQueryPort,
                deniedAuditLogPort,
                scanOverloadAndAlertUseCase
        );
    }

    @Bean
    @Primary
    public TransactionalNotificationDedupConfigServiceDecorator transactionalNotificationDedupConfigService(
            NotificationDedupConfigService delegate
    ) {
        return new TransactionalNotificationDedupConfigServiceDecorator(delegate);
    }
}
