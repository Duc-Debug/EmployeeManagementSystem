package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadProjectRoleAllocationStructurePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.SaveRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.allocation.template.RoleAllocationTemplateService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.template.TransactionalRoleAllocationTemplateServiceDecorator;

@Configuration
public class RoleAllocationTemplateUseCaseConfig {

    @Bean
    @Primary
    public TransactionalRoleAllocationTemplateServiceDecorator roleAllocationTemplateService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            SaveRoleAllocationTemplatePort saveTemplatePort,
            LoadRoleAllocationTemplatePort loadTemplatePort,
            LoadProjectRoleAllocationStructurePort loadStructurePort,
            LoadProjectPort loadProjectPort,
            LoadProjectRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadProjectResourceDemandPort loadDemandPort,
            SaveProjectResourceDemandPort saveDemandPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort) {

        RoleAllocationTemplateService pureJavaService = new RoleAllocationTemplateService(
                authorizationService,
                loadUserPort,
                saveTemplatePort,
                loadTemplatePort,
                loadStructurePort,
                loadProjectPort,
                loadRolePort,
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadDemandPort,
                saveDemandPort,
                saveAuditLogPort);

        return new TransactionalRoleAllocationTemplateServiceDecorator(pureJavaService);
    }
}

