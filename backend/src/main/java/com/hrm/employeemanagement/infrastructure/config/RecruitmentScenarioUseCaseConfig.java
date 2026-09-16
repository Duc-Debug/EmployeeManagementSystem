package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.DeleteSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadScenarioShortfallPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulationScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.SaveSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.scenario.recruitment.RecruitmentScenarioService;
import com.hrm.employeemanagement.infrastructure.transaction.scenario.recruitment.TransactionalRecruitmentScenarioUseCaseDecorator;

@Configuration
public class RecruitmentScenarioUseCaseConfig {

    @Bean
    @Primary
    public TransactionalRecruitmentScenarioUseCaseDecorator recruitmentScenarioService(
            AuthorizationService authorizationService,
            LoadSimulationScenarioPort loadScenarioPort,
            LoadScenarioShortfallPort loadShortfallPort,
            SaveSimulatedEmployeePort saveEmployeePort,
            LoadSimulatedEmployeePort loadEmployeePort,
            DeleteSimulatedEmployeePort deleteEmployeePort,
            LoadProjectRolePort loadRolePort,
            LoadSkillPort loadSkillPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        RecruitmentScenarioService service = new RecruitmentScenarioService(
                authorizationService,
                loadScenarioPort,
                loadShortfallPort,
                saveEmployeePort,
                loadEmployeePort,
                deleteEmployeePort,
                loadRolePort,
                loadSkillPort,
                saveAuditLogPort
        );
        return new TransactionalRecruitmentScenarioUseCaseDecorator(service);
    }
}
