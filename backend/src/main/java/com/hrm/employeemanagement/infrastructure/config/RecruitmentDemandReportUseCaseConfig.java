package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.report.GetRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.report.RecruitmentDemandReportService;

@Configuration
public class RecruitmentDemandReportUseCaseConfig {

    @Bean
    public GetRecruitmentDemandReportUseCase getRecruitmentDemandReportUseCase(
            AuthorizationService authorizationService,
            LoadRecruitmentDemandReportPort loadReportPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        return new RecruitmentDemandReportService(
                authorizationService,
                loadReportPort,
                saveAuditLogPort
        );
    }
}
