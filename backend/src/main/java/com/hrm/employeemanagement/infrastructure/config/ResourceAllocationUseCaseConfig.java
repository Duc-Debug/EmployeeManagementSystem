package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetProjectWeeklyAllocationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.SearchResourceBySkillAndAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.allocation.ResourceAllocationService;
import com.hrm.employeemanagement.application.service.allocation.GetProjectWeeklyAllocationsService;
import com.hrm.employeemanagement.application.service.allocation.SearchResourceBySkillAndAvailabilityService;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.RetryableAllocateResourceUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalAllocateResourceUseCase;

@Configuration
public class ResourceAllocationUseCaseConfig {

    @Bean
    public GetProjectWeeklyAllocationsUseCase getProjectWeeklyAllocationsUseCase(
            GetProjectDetailUseCase getProjectDetailUseCase,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            AuthorizationService authorizationService) {
        return new GetProjectWeeklyAllocationsService(getProjectDetailUseCase, loadAllocationPort, authorizationService);
    }

    @Bean
    public AllocateResourceUseCase allocateResourceUseCase(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort) {

        ResourceAllocationService pureService = new ResourceAllocationService(
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

        TransactionalAllocateResourceUseCase transactionalUseCase = new TransactionalAllocateResourceUseCase(pureService);
        return new RetryableAllocateResourceUseCaseDecorator(transactionalUseCase);
    }

    @Bean
    public SearchResourceBySkillAndAvailabilityUseCase searchResourceBySkillAndAvailabilityUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SearchResourcePort searchResourcePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort) {
        return new SearchResourceBySkillAndAvailabilityService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                searchResourcePort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAuditLogPort
        );
    }
}
