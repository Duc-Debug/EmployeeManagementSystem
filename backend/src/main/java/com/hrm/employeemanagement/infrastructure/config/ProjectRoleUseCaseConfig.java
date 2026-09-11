package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.outbound.project.CountProjectRoleUsagePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SyncEmployeeProfessionalRolePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillGroupPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.project.ProjectRoleManagementService;
import com.hrm.employeemanagement.infrastructure.transaction.project.TransactionalProjectRoleManagementServiceDecorator;

@Configuration
public class ProjectRoleUseCaseConfig {

    @Bean
    @Primary
    public TransactionalProjectRoleManagementServiceDecorator projectRoleManagementService(
            LoadProjectRolePort loadProjectRolePort,
            SaveProjectRolePort saveProjectRolePort,
            CountProjectRoleUsagePort countUsagePort,
            LoadSkillGroupPort loadSkillGroupPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            SyncEmployeeProfessionalRolePort syncEmployeeProfessionalRolePort) {

        ProjectRoleManagementService pureJavaService = new ProjectRoleManagementService(
                loadProjectRolePort,
                saveProjectRolePort,
                countUsagePort,
                loadSkillGroupPort,
                authorizationService,
                saveAuditLogPort,
                syncEmployeeProfessionalRolePort);

        return new TransactionalProjectRoleManagementServiceDecorator(pureJavaService);
    }
}
