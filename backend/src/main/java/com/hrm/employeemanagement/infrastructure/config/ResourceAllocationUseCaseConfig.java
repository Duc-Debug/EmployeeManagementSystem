package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.allocation.ResourceAllocationService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;

@Configuration
public class ResourceAllocationUseCaseConfig {

    @Bean
    public ResourceAllocationService resourceAllocationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort) {

        return new ResourceAllocationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadWeeklyAvailabilityPort,
                saveAllocationPort,
                loadAllocationPort,
                saveAuditLogPort,
                loadUserPort,
                loadOrgUnitPort
        );
    }
}
