package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.leave.GetEmployeeLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetMyLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.leave.GetEmployeeLeaveBalanceService;
import com.hrm.employeemanagement.application.service.leave.GetMyLeaveBalanceService;
import com.hrm.employeemanagement.application.service.leave.SubmitLeaveRequestService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalGetEmployeeLeaveBalanceService;
import com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalGetMyLeaveBalanceService;
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
            AuthorizationService authorizationService
    ) {
        GetMyLeaveBalanceService service = new GetMyLeaveBalanceService(
                loadEmployeePort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService
        );
        return new TransactionalGetMyLeaveBalanceService(service);
    }

    @Bean
    public GetEmployeeLeaveBalanceUseCase getEmployeeLeaveBalanceUseCase(
            LoadEmployeePort loadEmployeePort,
            com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort loadUserPort,
            com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort loadOrgUnitPort,
            LoadLeaveBalancePort loadLeaveBalancePort,
            SaveLeaveBalancePort saveLeaveBalancePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogRepository
    ) {
        GetEmployeeLeaveBalanceService service = new GetEmployeeLeaveBalanceService(
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                loadLeaveBalancePort,
                saveLeaveBalancePort,
                loadLeaveRequestPort,
                authorizationService,
                auditLogRepository
        );
        return new TransactionalGetEmployeeLeaveBalanceService(service);
    }

    @Bean
    public com.hrm.employeemanagement.application.port.inbound.leave.CancelLeaveRequestUseCase cancelLeaveRequestUseCase(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        com.hrm.employeemanagement.application.service.leave.CancelLeaveRequestService service =
                new com.hrm.employeemanagement.application.service.leave.CancelLeaveRequestService(
                        loadLeaveRequestPort,
                        saveLeaveRequestPort,
                        loadEmployeePort,
                        auditLogRepository,
                        authorizationService
                );
        return new com.hrm.employeemanagement.infrastructure.transaction.leave.TransactionalCancelLeaveRequestService(service);
    }
}
