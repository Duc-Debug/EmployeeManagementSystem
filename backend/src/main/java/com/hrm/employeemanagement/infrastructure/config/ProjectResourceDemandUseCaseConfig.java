package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.ProjectResourceDemandService;
import com.hrm.employeemanagement.infrastructure.transaction.project.RetryableEstimateResourceDemandUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalProjectResourceDemandServiceDecorator;

@Configuration
public class ProjectResourceDemandUseCaseConfig {

    @Bean
    public RetryableEstimateResourceDemandUseCaseDecorator projectResourceDemandService(
            LoadProjectPort loadProjectPort,
            LoadProjectRolePort loadProjectRolePort,
            LoadProjectResourceDemandPort loadDemandPort,
            SaveProjectResourceDemandPort saveDemandPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {

        ProjectResourceDemandService pureJavaService = new ProjectResourceDemandService(
                loadProjectPort,
                loadProjectRolePort,
                loadDemandPort,
                saveDemandPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);

        TransactionalProjectResourceDemandServiceDecorator transactionalDecorator =
                new TransactionalProjectResourceDemandServiceDecorator(pureJavaService);

        return new RetryableEstimateResourceDemandUseCaseDecorator(transactionalDecorator);
    }
}