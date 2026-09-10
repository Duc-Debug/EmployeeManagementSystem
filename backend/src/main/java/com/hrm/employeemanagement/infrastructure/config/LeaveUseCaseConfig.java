package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.leave.SubmitLeaveRequestService;
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
            AuthorizationService authorizationService
    ) {
        SubmitLeaveRequestService service = new SubmitLeaveRequestService(
                loadEmployeePort,
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                auditLogRepository,
                authorizationService
        );
        return new TransactionalSubmitLeaveRequestService(service);
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
