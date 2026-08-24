package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.ProjectService;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalProjectServiceDecorator;

@Configuration
public class ProjectUseCaseConfig {

    @Bean
    public TransactionalProjectServiceDecorator projectService(
            LoadProjectPort loadProjectPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService
    ) {
        ProjectService pureJavaProjectService =
                new ProjectService(
                        loadProjectPort,
                        loadUserPort,
                        loadEmployeePort,
                        saveDeniedAuditLogPort,
                        authorizationService
                );

        return new TransactionalProjectServiceDecorator(
                pureJavaProjectService
        );
    }
}
