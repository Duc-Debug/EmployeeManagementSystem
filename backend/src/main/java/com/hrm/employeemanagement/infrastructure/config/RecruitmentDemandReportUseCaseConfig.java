package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.GetRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.RecruitmentDemandReportService;

@Configuration
public class RecruitmentDemandReportUseCaseConfig {

    @Bean
    public GetRecruitmentDemandReportUseCase getRecruitmentDemandReportUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadRecruitmentDemandReportPort loadReportPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        return new RecruitmentDemandReportService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadReportPort,
                saveAuditLogPort
        );
    }
}
