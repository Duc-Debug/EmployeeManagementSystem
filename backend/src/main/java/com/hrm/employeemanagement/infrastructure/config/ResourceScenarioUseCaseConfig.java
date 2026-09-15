package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.scenario.*;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.*;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.scenario.ResourceScenarioService;
import com.hrm.employeemanagement.application.service.scenario.ScenarioDemandService;
import com.hrm.employeemanagement.application.service.scenario.ScenarioSimulationCalculationService;
import com.hrm.employeemanagement.infrastructure.transaction.scenario.TransactionalResourceScenarioServiceDecorator;

@Configuration
public class ResourceScenarioUseCaseConfig {

    @Bean
    public TransactionalResourceScenarioServiceDecorator resourceScenarioServiceDecorator(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveResourceScenarioPort saveScenarioPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveScenarioSnapshotPort saveSnapshotPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            SaveScenarioDemandPort saveDemandPort,
            LoadScenarioDemandPort loadDemandPort,
            DeleteScenarioDemandPort deleteDemandPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        ResourceScenarioService scenarioService = new ResourceScenarioService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                saveScenarioPort,
                loadScenarioPort,
                saveSnapshotPort,
                loadSnapshotPort,
                loadDemandPort,
                saveAuditLogPort
        );

        ScenarioDemandService demandService = new ScenarioDemandService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadScenarioPort,
                saveDemandPort,
                loadDemandPort,
                deleteDemandPort,
                saveAuditLogPort
        );

        ScenarioSimulationCalculationService simulationService = new ScenarioSimulationCalculationService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadScenarioPort,
                loadDemandPort,
                loadSnapshotPort,
                loadCapacityThresholdPort,
                saveAuditLogPort
        );

        return new TransactionalResourceScenarioServiceDecorator(
                scenarioService,
                scenarioService,
                scenarioService,
                demandService,
                demandService,
                demandService,
                simulationService
        );
    }
}
