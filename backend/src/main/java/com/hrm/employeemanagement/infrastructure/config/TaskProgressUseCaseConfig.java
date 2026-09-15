package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskProgressUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.task.UpdateTaskProgressService;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalUpdateTaskProgressUseCase;

@Configuration
public class TaskProgressUseCaseConfig {

    @Bean
    public UpdateTaskProgressUseCase updateTaskProgressUseCase(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort) {
        UpdateTaskProgressService pureService = new UpdateTaskProgressService(
                loadTaskPort,
                saveTaskPort,
                loadTaskAssignmentPort,
                loadProjectPort,
                loadEmployeePort,
                authenticatedUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort);
        return new TransactionalUpdateTaskProgressUseCase(pureService);
    }
}
