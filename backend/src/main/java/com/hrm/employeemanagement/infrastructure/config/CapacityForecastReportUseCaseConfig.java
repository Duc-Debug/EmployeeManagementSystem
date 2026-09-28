package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.report.capacityforecast.GetCapacityForecastUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.capacityforecast.GetCapacityForecastService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class CapacityForecastReportUseCaseConfig {

    @Bean
    public GetCapacityForecastUseCase getCapacityForecastUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            Optional<LoadWorkingCalendarPort> loadWorkingCalendarPort,
            Optional<LoadResourceReservationPort> loadReservationPort,
            Optional<SaveAuditLogPort> saveAuditLogPort
    ) {
        return new GetCapacityForecastService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadAllocationPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort.orElse(null),
                loadReservationPort.orElse(null),
                saveAuditLogPort.orElse(null)
        );
    }
}

