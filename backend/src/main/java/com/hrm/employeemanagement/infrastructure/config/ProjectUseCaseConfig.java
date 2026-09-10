package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.CreateProjectFromTemplateService;
import com.hrm.employeemanagement.application.service.project.CreateProjectService;
import com.hrm.employeemanagement.application.service.project.ProjectService;
import com.hrm.employeemanagement.application.service.project.UpdateProjectService;
import com.hrm.employeemanagement.infrastructure.transaction.project.RetryableCreateProjectUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalCreateProjectUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalProjectServiceDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalUpdateProjectUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.projecttemplate.RetryableCreateProjectFromTemplateUseCaseDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.projecttemplate.TransactionalCreateProjectFromTemplateUseCase;

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

        @Bean
        public UpdateProjectUseCase updateProjectUseCase(
                        LoadProjectPort loadProjectPort,
                        SaveProjectPort saveProjectPort,
                        LoadOrgUnitPort loadOrgUnitPort,
                        LoadEmployeePort loadEmployeePort,
                        LoadUserPort loadUserPort,
                        SaveAuditLogPort saveAuditLogPort,
                        SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
                        AuthorizationService authorizationService) {
                UpdateProjectService pureService = new UpdateProjectService(
                                loadProjectPort,
                                saveProjectPort,
                                loadOrgUnitPort,
                                loadEmployeePort,
                                loadUserPort,
                                saveAuditLogPort,
                                saveDeniedAuditLogPort,
                                authorizationService);
                return new TransactionalUpdateProjectUseCase(pureService);
        }

        @Bean
        public CreateProjectFromTemplateUseCase createProjectFromTemplateUseCase(
                        LoadProjectTemplatePort loadProjectTemplatePort,
                        SaveProjectPort saveProjectPort,
                        SaveTaskPort saveTaskPort,
                        LoadOrgUnitPort loadOrgUnitPort,
                        LoadEmployeePort loadEmployeePort,
                        LoadUserPort loadUserPort,
                        SaveAuditLogPort saveAuditLogPort,
                        SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
                        AuthorizationService authorizationService) {
                CreateProjectFromTemplateService pureService = new CreateProjectFromTemplateService(
                                loadProjectTemplatePort,
                                saveProjectPort,
                                saveTaskPort,
                                loadOrgUnitPort,
                                loadEmployeePort,
                                loadUserPort,
                                saveAuditLogPort,
                                saveDeniedAuditLogPort,
                                authorizationService);
                TransactionalCreateProjectFromTemplateUseCase transactionalUseCase = new TransactionalCreateProjectFromTemplateUseCase(pureService);
                return new RetryableCreateProjectFromTemplateUseCaseDecorator(transactionalUseCase);
        }
}
