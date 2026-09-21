package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.unavailability.ApproveUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.CancelUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.CheckUnavailabilityConflictService;
import com.hrm.employeemanagement.application.service.unavailability.GetUnavailabilityDeclarationsService;
import com.hrm.employeemanagement.application.service.unavailability.RejectUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.SubmitUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.UnavailabilityDataScopeValidator;
import com.hrm.employeemanagement.infrastructure.transaction.unavailability.TransactionalUnavailabilityServiceDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnavailabilityUseCaseConfig {

    @Bean
    public TransactionalUnavailabilityServiceDecorator unavailabilityServiceDecorator(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            SaveUnavailabilityDeclarationPort saveUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        SubmitUnavailabilityDeclarationService submitService = new SubmitUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveAuditLogPort,
                loadWorkingCalendarPort
        );

        ApproveUnavailabilityDeclarationService approveService = new ApproveUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService,
                saveAuditLogPort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort
        );

        RejectUnavailabilityDeclarationService rejectService = new RejectUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService,
                saveAuditLogPort
        );

        CancelUnavailabilityDeclarationService cancelService = new CancelUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveAuditLogPort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort
        );

        GetUnavailabilityDeclarationsService getService = new GetUnavailabilityDeclarationsService(
                loadUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService
        );

        UnavailabilityDataScopeValidator dataScopeValidator = new UnavailabilityDataScopeValidator(loadOrgUnitPort);

        CheckUnavailabilityConflictService checkService = new CheckUnavailabilityConflictService(
                loadUnavailabilityPort,
                loadAllocationPort,
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                dataScopeValidator
        );

        return new TransactionalUnavailabilityServiceDecorator(
                submitService,
                approveService,
                rejectService,
                cancelService,
                getService,
                checkService
        );
    }
}
