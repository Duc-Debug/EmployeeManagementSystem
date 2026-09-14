package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.task.GetProjectTaskTrackingUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.GetProjectTaskTrackingService;

@Configuration
public class TaskTrackingUseCaseConfig {

    @Bean
    public GetProjectTaskTrackingUseCase getProjectTaskTrackingUseCase(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort) {
        return new GetProjectTaskTrackingService(
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveDeniedAuditLogPort
        );
    }
}
