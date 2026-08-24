package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.orgunit.OrgUnitService;
import com.hrm.employeemanagement.infrastructure.transaction.orgunit.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrgUnitUseCaseConfig {
    @Bean("orgUnitService")
    public OrgUnitService orgUnitService(LoadOrgUnitPort loadOrgUnitPort, SaveOrgUnitPort saveOrgUnitPort,
            SaveAuditLogPort saveAuditLogPort, CurrentUserPort currentUserPort) {
        return new OrgUnitService(loadOrgUnitPort, saveOrgUnitPort, saveAuditLogPort, currentUserPort);
    }

    @Bean("transactionalCreateOrgUnitUseCase")
    public CreateOrgUnitUseCase createOrgUnitUseCase(OrgUnitService orgUnitService) {
        return new TransactionalCreateOrgUnitUseCase(orgUnitService);
    }

    @Bean("transactionalUpdateOrgUnitUseCase")
    public UpdateOrgUnitUseCase updateOrgUnitUseCase(OrgUnitService orgUnitService) {
        return new TransactionalUpdateOrgUnitUseCase(orgUnitService);
    }

    @Bean("transactionalMoveOrgUnitUseCase")
    public MoveOrgUnitUseCase moveOrgUnitUseCase(OrgUnitService orgUnitService) {
        return new TransactionalMoveOrgUnitUseCase(orgUnitService);
    }

    @Bean("transactionalDeactivateOrgUnitUseCase")
    public DeactivateOrgUnitUseCase deactivateOrgUnitUseCase(OrgUnitService orgUnitService) {
        return new TransactionalDeactivateOrgUnitUseCase(orgUnitService);
    }

    @Bean("getOrgTreeUseCase")
    public GetOrgTreeUseCase getOrgTreeUseCase(OrgUnitService orgUnitService) {
        return orgUnitService;
    }
}