package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.leave.ApproveLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.CancelLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetEmployeeLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetLeaveImpactUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetMyLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetPendingLeaveRequestsUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.RejectLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadProjectAllocationForLeavePort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.leave.ApproveLeaveRequestService;
import com.hrm.employeemanagement.application.service.leave.CancelLeaveRequestService;
import com.hrm.employeemanagement.application.service.leave.GetEmployeeLeaveBalanceService;
import com.hrm.employeemanagement.application.service.leave.GetLeaveImpactService;
import com.hrm.employeemanagement.application.service.leave.GetMyLeaveBalanceService;
import com.hrm.employeemanagement.application.service.leave.GetPendingLeaveRequestsService;
import com.hrm.employeemanagement.application.service.leave.RejectLeaveRequestService;
import com.hrm.employeemanagement.application.service.leave.SubmitLeaveRequestService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalApproveLeaveRequestService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalCancelLeaveRequestService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalGetEmployeeLeaveBalanceService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalGetMyLeaveBalanceService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalRejectLeaveRequestService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalSubmitLeaveRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeaveUseCaseConfig {

    @Bean
    public SubmitLeaveRequestUseCase submitLeaveRequestUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadLeaveBalancePort loadLeaveBalancePort
    ) {
        SubmitLeaveRequestService service = new SubmitLeaveRequestService(
                loadEmployeePort,
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                auditLogRepository,
                authorizationService,
                loadWorkingCalendarPort,
                loadHolidaysPort,
                loadLeaveBalancePort
        );
        return new TransactionalSubmitLeaveRequestService(service);
    }

    @Bean
    public GetMyLeaveBalanceUseCase getMyLeaveBalanceUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort
    ) {
        GetMyLeaveBalanceService service = new GetMyLeaveBalanceService(
                loadEmployeePort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService,
                loadWorkingCalendarPort,
                loadHolidaysPort
        );
        return new TransactionalGetMyLeaveBalanceService(service);
    }

    @Bean
    public GetEmployeeLeaveBalanceUseCase getEmployeeLeaveBalanceUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort
    ) {
        GetEmployeeLeaveBalanceService service = new GetEmployeeLeaveBalanceService(
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService,
                auditLogRepository,
                loadWorkingCalendarPort,
                loadHolidaysPort
        );
        return new TransactionalGetEmployeeLeaveBalanceService(service);
    }

    @Bean
    public CancelLeaveRequestUseCase cancelLeaveRequestUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        CancelLeaveRequestService service = new CancelLeaveRequestService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                loadEmployeePort,
                auditLogRepository,
                authorizationService
        );
        return new TransactionalCancelLeaveRequestService(service);
    }

    @Bean
    public GetPendingLeaveRequestsUseCase getPendingLeaveRequestsUseCase(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService
    ) {
        return new GetPendingLeaveRequestsService(
                loadLeaveRequestPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService
        );
    }

    @Bean
    public ApproveLeaveRequestUseCase approveLeaveRequestUseCase(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            LoadWeeklyProjectAllocationPort loadWeeklyProjectAllocationPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            SaveWeeklyProjectAllocationPort saveWeeklyProjectAllocationPort
    ) {
        ApproveLeaveRequestService service = new ApproveLeaveRequestService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                saveLeaveAuditLogPort,
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadWorkingCalendarPort,
                loadWeeklyProjectAllocationPort,
                saveWeeklyProjectAllocationPort
        );
        return new TransactionalApproveLeaveRequestService(service);
    }

    @Bean
    public RejectLeaveRequestUseCase rejectLeaveRequestUseCase(
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveLeaveAuditLogPort saveLeaveAuditLogPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort
    ) {
        RejectLeaveRequestService service = new RejectLeaveRequestService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                saveLeaveAuditLogPort,
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort
        );
        return new TransactionalRejectLeaveRequestService(service);
    }

    @Bean
    public GetLeaveImpactUseCase getLeaveImpactUseCase(
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectAllocationForLeavePort loadProjectAllocationPort,
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        return new GetLeaveImpactService(
                loadLeaveRequestPort,
                loadEmployeePort,
                loadProjectAllocationPort,
                authorizationService,
                loadUserPort,
                loadOrgUnitPort
        );
    }
}
