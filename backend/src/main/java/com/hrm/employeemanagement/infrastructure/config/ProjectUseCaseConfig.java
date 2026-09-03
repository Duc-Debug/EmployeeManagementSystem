package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.CreateProjectService;
import com.hrm.employeemanagement.application.service.project.ProjectService;
import com.hrm.employeemanagement.infrastructure.transaction.project.RetryableCreateProjectUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalCreateProjectUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalProjectServiceDecorator;

@Configuration
public class ProjectUseCaseConfig {

        @Bean
        public TransactionalProjectServiceDecorator projectService(
                        LoadProjectPort loadProjectPort,
                        LoadUserPort loadUserPort,
                        LoadEmployeePort loadEmployeePort,
                        SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
                        AuthorizationService authorizationService) {
                ProjectService pureJavaProjectService = new ProjectService(
                                loadProjectPort,
                                loadUserPort,
                                loadEmployeePort,
                                saveDeniedAuditLogPort,
                                authorizationService);

                return new TransactionalProjectServiceDecorator(
                                pureJavaProjectService);
        }

        @Bean
        public CreateProjectUseCase createProjectUseCase(
                        SaveProjectPort saveProjectPort,
                        LoadOrgUnitPort loadOrgUnitPort,
                        LoadEmployeePort loadEmployeePort,
                        LoadUserPort loadUserPort,
                        SaveAuditLogPort saveAuditLogPort,
                        SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
                        AuthorizationService authorizationService) {
                CreateProjectService pureService = new CreateProjectService(
                                saveProjectPort,
                                loadOrgUnitPort,
                                loadEmployeePort,
                                loadUserPort,
                                saveAuditLogPort,
                                saveDeniedAuditLogPort,
                                authorizationService);
                TransactionalCreateProjectUseCase transactionalUseCase = new TransactionalCreateProjectUseCase(pureService);
                return new RetryableCreateProjectUseCaseDecorator(transactionalUseCase);
        }
}
