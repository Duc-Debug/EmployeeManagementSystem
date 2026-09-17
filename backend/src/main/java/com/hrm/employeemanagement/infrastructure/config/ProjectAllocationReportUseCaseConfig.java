package com.hrm.employeemanagement.infrastructure.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.ExportProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.GetProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.projectallocation.GetProjectAllocationReportService;

@Configuration
public class ProjectAllocationReportUseCaseConfig {

    @Bean
    public GetProjectAllocationReportService projectAllocationReportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadProjectResourceDemandPort loadDemandPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadProjectRolePort loadProjectRolePort,
            Optional<LoadProjectMemberPort> loadProjectMemberPort,
            Optional<SaveAuditLogPort> saveAuditLogPort
    ) {
        return new GetProjectAllocationReportService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadDemandPort,
                loadAllocationPort,
                loadProjectRolePort,
                loadProjectMemberPort.orElse(null),
                saveAuditLogPort.orElse(null)
        );
    }
}
