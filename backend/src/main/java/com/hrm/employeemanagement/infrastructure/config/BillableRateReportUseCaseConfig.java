package com.hrm.employeemanagement.infrastructure.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.billablerate.ExportBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.GetBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.billablerate.GetBillableRateReportService;

@Configuration
public class BillableRateReportUseCaseConfig {

    @Bean
    public GetBillableRateReportService billableRateReportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadBillableRateTimesheetPort loadBillableRateTimesheetPort,
            Optional<LoadWorkingCalendarPort> loadWorkingCalendarPort,
            Optional<SaveAuditLogPort> saveAuditLogPort
    ) {
        return new GetBillableRateReportService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadBillableRateTimesheetPort,
                loadWorkingCalendarPort.orElse(null),
                saveAuditLogPort.orElse(null)
        );
    }

    @Bean
    public GetBillableRateReportUseCase getBillableRateReportUseCase(GetBillableRateReportService billableRateReportService) {
        return billableRateReportService;
    }

    @Bean
    public ExportBillableRateReportUseCase exportBillableRateReportUseCase(GetBillableRateReportService billableRateReportService) {
        return billableRateReportService;
    }
}
