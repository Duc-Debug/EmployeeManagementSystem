package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.skill.DeclareEmployeeSkillService;
import com.hrm.employeemanagement.infrastructure.transaction.skill.TransactionalDeclareEmployeeSkillService;

@Configuration
public class SkillUseCaseConfig {

    @Bean
    public DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        DeclareEmployeeSkillService service = new DeclareEmployeeSkillService(
                employeeSkillRepository,
                skillCatalogRepository,
                auditLogRepository,
                authorizationService
        );
        return new TransactionalDeclareEmployeeSkillService(service);
    }
}
