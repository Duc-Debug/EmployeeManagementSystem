package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.SaveCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.allocation.threshold.CapacityThresholdService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.threshold.TransactionalCapacityThresholdServiceDecorator;

@Configuration
public class CapacityThresholdUseCaseConfig {

    @Bean
    public TransactionalCapacityThresholdServiceDecorator capacityThresholdServiceDecorator(
            AuthorizationService authorizationService,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            SaveCapacityThresholdPort saveCapacityThresholdPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadCapacityThresholdHistoryPort historyPort,
            LoadUserPort loadUserPort,
            com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort loadOrgUnitPort
    ) {
        CapacityThresholdService pureService = new CapacityThresholdService(
                authorizationService,
                loadCapacityThresholdPort,
                saveCapacityThresholdPort,
                saveAuditLogPort,
                historyPort,
                loadUserPort,
                loadOrgUnitPort
        );

        return new TransactionalCapacityThresholdServiceDecorator(pureService, pureService, pureService);
    }
}
