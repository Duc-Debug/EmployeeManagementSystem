package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.availability.WeeklyAvailabilityService;
import com.hrm.employeemanagement.infrastructure.transaction.availability.TransactionalWeeklyAvailabilityServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeeklyAvailabilityUseCaseConfig {

    @Bean
    public TransactionalWeeklyAvailabilityServiceDecorator weeklyAvailabilityService(
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            AuthorizationService authorizationService) {
        WeeklyAvailabilityService service = new WeeklyAvailabilityService(
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                authorizationService
        );
        return new TransactionalWeeklyAvailabilityServiceDecorator(service);
    }
}
