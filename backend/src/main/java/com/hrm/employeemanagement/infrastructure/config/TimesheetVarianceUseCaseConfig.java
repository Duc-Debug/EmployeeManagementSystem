package com.hrm.employeemanagement.infrastructure.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance.GetTimesheetVarianceUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.timesheetvariance.LoadTimesheetVariancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.timesheetvariance.GetTimesheetVarianceService;
import com.hrm.employeemanagement.infrastructure.transaction.report.timesheetvariance.TransactionalTimesheetVarianceUseCaseDecorator;

@Configuration
public class TimesheetVarianceUseCaseConfig {

    @Bean
    public GetTimesheetVarianceUseCase getTimesheetVarianceUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadTimesheetVariancePort loadTimesheetVariancePort,
            Optional<SaveAuditLogPort> saveAuditLogPort
    ) {
        GetTimesheetVarianceService service = new GetTimesheetVarianceService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadAllocationPort,
                loadTimesheetVariancePort,
                saveAuditLogPort.orElse(null)
        );
        return new TransactionalTimesheetVarianceUseCaseDecorator(service);
    }
}
