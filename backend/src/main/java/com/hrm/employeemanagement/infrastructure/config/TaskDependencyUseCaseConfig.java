package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.DeleteTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.TaskDependencyService;
import com.hrm.employeemanagement.infrastructure.transaction.task.TransactionalTaskDependencyServiceDecorator;

@Configuration
public class TaskDependencyUseCaseConfig {

    @Bean
    public TransactionalTaskDependencyServiceDecorator taskDependencyService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskDependencyPort loadDependencyPort,
            SaveTaskDependencyPort saveDependencyPort,
            DeleteTaskDependencyPort deleteDependencyPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {

        TaskDependencyService pureJavaService = new TaskDependencyService(
                loadProjectPort,
                loadTaskPort,
                loadDependencyPort,
                saveDependencyPort,
                deleteDependencyPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );

        return new TransactionalTaskDependencyServiceDecorator(pureJavaService);
    }
}
