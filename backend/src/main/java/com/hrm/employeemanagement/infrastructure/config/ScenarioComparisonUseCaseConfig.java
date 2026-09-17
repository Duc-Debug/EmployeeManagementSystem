package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.scenario.CompareSimulationScenariosUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.application.service.scenario.ScenarioComparisonService;

@Configuration
public class ScenarioComparisonUseCaseConfig {

    @Bean
    public CompareSimulationScenariosUseCase compareSimulationScenariosUseCase(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadResourceScenarioPort loadScenarioPort,
            LoadOrgUnitPort loadOrgUnitPort,
            GetScenarioSimulationResultUseCase simulationResultUseCase,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        ScenarioComparisonService service = new ScenarioComparisonService(
                authorizationService,
                loadUserPort,
                loadScenarioPort,
                loadOrgUnitPort,
                simulationResultUseCase,
                saveAuditLogPort
        );
        return new com.hrm.employeemanagement.infrastructure.transaction.scenario.TransactionalScenarioComparisonUseCaseDecorator(service);
    }
}
