package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.scenario.ApplyResourceScenarioService;
import com.hrm.employeemanagement.application.service.scenario.ScenarioBaselineValidator;
import com.hrm.employeemanagement.infrastructure.transaction.scenario.TransactionalApplyResourceScenarioServiceDecorator;

@Configuration
public class ApplyResourceScenarioUseCaseConfig {

    @Bean
    @Primary
    public TransactionalApplyResourceScenarioServiceDecorator applyResourceScenarioService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadResourceScenarioPort loadScenarioPort,
            SaveResourceScenarioPort saveScenarioPort,
            LoadScenarioDemandPort loadDemandPort,
            LoadScenarioSnapshotPort loadSnapshotPort,
            SaveScenarioSnapshotPort saveSnapshotPort,
            DeleteScenarioSnapshotPort deleteSnapshotPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            @Autowired(required = false) LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveAuditLogPort saveAuditLogPort,
            ScenarioBaselineValidator scenarioBaselineValidator
    ) {
        ApplyResourceScenarioService service = new ApplyResourceScenarioService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadScenarioPort,
                saveScenarioPort,
                loadDemandPort,
                loadSnapshotPort,
                saveSnapshotPort,
                deleteSnapshotPort,
                loadAllocationPort,
                saveAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                saveAuditLogPort,
                scenarioBaselineValidator
        );

        return new TransactionalApplyResourceScenarioServiceDecorator(service);
    }

    @Bean
    public ScenarioBaselineValidator scenarioBaselineValidator(
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            @Autowired(required = false) LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        return new ScenarioBaselineValidator(
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort
        );
    }
}

