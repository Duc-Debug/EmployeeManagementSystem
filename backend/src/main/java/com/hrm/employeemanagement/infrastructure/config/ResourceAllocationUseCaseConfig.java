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

import com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.service.allocation.GetCompanyWeeklyCapacityService;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;

@Configuration
public class ResourceAllocationUseCaseConfig {

    @Bean
    public GetCompanyWeeklyCapacityUseCase getCompanyWeeklyCapacityUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            java.util.Optional<LoadWorkingCalendarPort> loadWorkingCalendarPort) {
        return new GetCompanyWeeklyCapacityService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort.orElse(null)
        );
    }

    @Bean
    public GetProjectWeeklyAllocationsUseCase getProjectWeeklyAllocationsUseCase(
            GetProjectDetailUseCase getProjectDetailUseCase,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            AuthorizationService authorizationService) {
        return new GetProjectWeeklyAllocationsService(getProjectDetailUseCase, loadAllocationPort, authorizationService);
    }

    @Bean
    public ResourceAllocationService resourceAllocationPureService(
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

    @Bean
    public TransactionalAllocateResourceUseCase transactionalAllocateResourceUseCase(
            ResourceAllocationService resourceAllocationPureService) {
        return new TransactionalAllocateResourceUseCase(resourceAllocationPureService);
    }

    @Bean
    public AllocateResourceUseCase allocateResourceUseCase(
            TransactionalAllocateResourceUseCase transactionalAllocateResourceUseCase) {
        return new RetryableAllocateResourceUseCaseDecorator(transactionalAllocateResourceUseCase);
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
