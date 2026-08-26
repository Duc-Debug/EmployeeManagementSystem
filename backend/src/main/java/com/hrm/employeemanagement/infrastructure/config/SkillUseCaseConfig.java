package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.skill.*;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.skill.*;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.skill.SkillService;
import com.hrm.employeemanagement.infrastructure.transaction.skill.*;

@Configuration
public class SkillUseCaseConfig {

    @Bean("skillService")
    public SkillService skillService(
            LoadSkillPort loadSkillPort,
            SaveSkillPort saveSkillPort,
            LoadSkillGroupPort loadSkillGroupPort,
            SaveSkillGroupPort saveSkillGroupPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService,
            CurrentUserPort currentUserPort) {
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
        return new TransactionalCreateSkillUseCase(skillService);
    }

    @Bean("transactionalUpdateSkillUseCase")
    public UpdateSkillUseCase updateSkillUseCase(SkillService skillService) {
        return new TransactionalUpdateSkillUseCase(skillService);
    }

    @Bean("transactionalMergeSkillUseCase")
    public MergeSkillUseCase mergeSkillUseCase(SkillService skillService) {
        return new TransactionalMergeSkillUseCase(skillService);
    }

    @Bean("transactionalDeactivateSkillUseCase")
    public DeactivateSkillUseCase deactivateSkillUseCase(SkillService skillService) {
        return new TransactionalDeactivateSkillUseCase(skillService);
    }

    @Bean("getSkillListUseCase")
    public GetSkillListUseCase getSkillListUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("getSkillGroupListUseCase")
    public GetSkillGroupListUseCase getSkillGroupListUseCase(SkillService skillService) {
        return skillService;
    }

    @Bean("transactionalCreateSkillGroupUseCase")
    public CreateSkillGroupUseCase createSkillGroupUseCase(SkillService skillService) {
        return new TransactionalCreateSkillGroupUseCase(skillService);
    }

    @Bean("transactionalUpdateSkillGroupUseCase")
    public UpdateSkillGroupUseCase updateSkillGroupUseCase(SkillService skillService) {
        return new TransactionalUpdateSkillGroupUseCase(skillService);
    }

    @Bean("transactionalDeactivateSkillGroupUseCase")
    public DeactivateSkillGroupUseCase deactivateSkillGroupUseCase(SkillService skillService) {
        return new TransactionalDeactivateSkillGroupUseCase(skillService);
    }
}