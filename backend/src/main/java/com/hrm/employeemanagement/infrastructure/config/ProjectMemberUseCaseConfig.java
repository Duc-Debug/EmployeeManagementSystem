package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.project.AddProjectMemberUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectMembersUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.RemoveProjectMemberUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.AddProjectMemberService;
import com.hrm.employeemanagement.application.service.project.GetProjectMembersService;
import com.hrm.employeemanagement.application.service.project.RemoveProjectMemberService;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalAddProjectMemberUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalGetProjectMembersUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalRemoveProjectMemberUseCase;

@Configuration
public class ProjectMemberUseCaseConfig {

    @Bean
    public GetProjectMembersUseCase getProjectMembersUseCase(
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        GetProjectMembersService pureService = new GetProjectMembersService(
                loadProjectPort,
                loadProjectMemberPort,
                loadUserPort,
                loadEmployeePort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalGetProjectMembersUseCase(pureService);
    }

    @Bean
    public AddProjectMemberUseCase addProjectMemberUseCase(
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        AddProjectMemberService pureService = new AddProjectMemberService(
                loadProjectPort,
                loadProjectMemberPort,
                saveProjectMemberPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalAddProjectMemberUseCase(pureService);
    }

    @Bean
    public RemoveProjectMemberUseCase removeProjectMemberUseCase(
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        RemoveProjectMemberService pureService = new RemoveProjectMemberService(
                loadProjectPort,
                loadProjectMemberPort,
                saveProjectMemberPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
        return new TransactionalRemoveProjectMemberUseCase(pureService);
    }
}
