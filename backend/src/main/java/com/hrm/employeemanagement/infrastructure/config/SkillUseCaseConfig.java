package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.skill.*;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.skill.*;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.skill.DeclareEmployeeSkillService;
import com.hrm.employeemanagement.application.service.skill.SkillService;
import com.hrm.employeemanagement.infrastructure.transaction.skill.TransactionalDeclareEmployeeSkillService;

@Configuration
public class SkillUseCaseConfig {

    @Bean
    public DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        DeclareEmployeeSkillService service = new DeclareEmployeeSkillService(
                employeeSkillRepository,
                skillCatalogRepository,
                auditLogRepository,
                loadEmployeePort,
                authorizationService
        );
        return new TransactionalDeclareEmployeeSkillService(service);
    }

    @Bean
    public SkillService skillService(
            LoadSkillPort loadSkillPort,
            SaveSkillPort saveSkillPort,
            LoadSkillGroupPort loadSkillGroupPort,
            SaveSkillGroupPort saveSkillGroupPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService,
            CurrentUserPort currentUserPort
    ) {
        return new SkillService(
                loadSkillPort,
                saveSkillPort,
                loadSkillGroupPort,
                saveSkillGroupPort,
                saveAuditLogPort,
                authorizationService,
                currentUserPort
        );
    }

    @Bean("transactionalCreateSkillUseCase")
    public CreateSkillUseCase createSkillUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalUpdateSkillUseCase")
    public UpdateSkillUseCase updateSkillUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalMergeSkillUseCase")
    public MergeSkillUseCase mergeSkillUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalDeactivateSkillUseCase")
    public DeactivateSkillUseCase deactivateSkillUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean
    public GetSkillListUseCase getSkillListUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean
    public GetSkillGroupListUseCase getSkillGroupListUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalCreateSkillGroupUseCase")
    public CreateSkillGroupUseCase createSkillGroupUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalUpdateSkillGroupUseCase")
    public UpdateSkillGroupUseCase updateSkillGroupUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalDeactivateSkillGroupUseCase")
    public DeactivateSkillGroupUseCase deactivateSkillGroupUseCase(SkillService skillService) {
        return skillService;
    }
}
