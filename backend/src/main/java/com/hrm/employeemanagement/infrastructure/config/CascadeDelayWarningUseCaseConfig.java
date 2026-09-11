package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.task.CascadeDelayWarningService;

@Configuration
public class CascadeDelayWarningUseCaseConfig {

    @Bean
    public CascadeDelayWarningService cascadeDelayWarningService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskDependencyPort loadDependencyPort,
            LoadMilestonePort loadMilestonePort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {

        return new CascadeDelayWarningService(
                loadProjectPort,
                loadTaskPort,
                saveTaskPort,
                loadDependencyPort,
                loadMilestonePort,
                saveAuditLogPort,
                authorizationService
        );
    }
}
