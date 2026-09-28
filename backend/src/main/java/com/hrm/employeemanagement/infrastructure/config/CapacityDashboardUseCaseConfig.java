package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.dashboard.capacity.GetCapacityDashboardUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.dashboard.capacity.GetCapacityDashboardService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class CapacityDashboardUseCaseConfig {

    @Bean
    public GetCapacityDashboardUseCase getCapacityDashboardUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            Optional<LoadWorkingCalendarPort> loadWorkingCalendarPort,
            Optional<LoadScheduleConflictPort> loadScheduleConflictPort,
            Optional<LoadProjectPort> loadProjectPort,
            Optional<LoadProjectMemberPort> loadProjectMemberPort,
            Optional<LoadCapacityThresholdPort> loadCapacityThresholdPort,
            Optional<SaveAuditLogPort> saveAuditLogPort
    ) {
        return new GetCapacityDashboardService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort.orElse(null),
                loadScheduleConflictPort.orElse(null),
                loadProjectPort.orElse(null),
                loadProjectMemberPort.orElse(null),
                loadCapacityThresholdPort.orElse(null),
                saveAuditLogPort.orElse(null)
        );
    }
}
