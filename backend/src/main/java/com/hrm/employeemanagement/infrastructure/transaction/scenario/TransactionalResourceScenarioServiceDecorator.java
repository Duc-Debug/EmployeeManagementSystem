package com.hrm.employeemanagement.infrastructure.transaction.scenario;

import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.scenario.*;
import com.hrm.employeemanagement.application.port.inbound.scenario.*;

public class TransactionalResourceScenarioServiceDecorator implements
        CreateSimulationScenarioUseCase,
        GetSimulationScenarioUseCase,
        ListSimulationScenariosUseCase,
        AddScenarioDemandUseCase,
        UpdateScenarioDemandUseCase,
        DeleteScenarioDemandUseCase,
        GetScenarioSimulationResultUseCase {

    private final CreateSimulationScenarioUseCase createScenarioUseCase;
    private final GetSimulationScenarioUseCase getScenarioUseCase;
    private final ListSimulationScenariosUseCase listScenariosUseCase;
    private final AddScenarioDemandUseCase addDemandUseCase;
    private final UpdateScenarioDemandUseCase updateDemandUseCase;
    private final DeleteScenarioDemandUseCase deleteDemandUseCase;
    private final GetScenarioSimulationResultUseCase simulationResultUseCase;

    public TransactionalResourceScenarioServiceDecorator(
            CreateSimulationScenarioUseCase createScenarioUseCase,
            GetSimulationScenarioUseCase getScenarioUseCase,
            ListSimulationScenariosUseCase listScenariosUseCase,
            AddScenarioDemandUseCase addDemandUseCase,
            UpdateScenarioDemandUseCase updateDemandUseCase,
            DeleteScenarioDemandUseCase deleteDemandUseCase,
            GetScenarioSimulationResultUseCase simulationResultUseCase
    ) {
        this.createScenarioUseCase = Objects.requireNonNull(createScenarioUseCase, "CreateSimulationScenarioUseCase must not be null");
        this.getScenarioUseCase = Objects.requireNonNull(getScenarioUseCase, "GetSimulationScenarioUseCase must not be null");
        this.listScenariosUseCase = Objects.requireNonNull(listScenariosUseCase, "ListSimulationScenariosUseCase must not be null");
        this.addDemandUseCase = Objects.requireNonNull(addDemandUseCase, "AddScenarioDemandUseCase must not be null");
        this.updateDemandUseCase = Objects.requireNonNull(updateDemandUseCase, "UpdateScenarioDemandUseCase must not be null");
        this.deleteDemandUseCase = Objects.requireNonNull(deleteDemandUseCase, "DeleteScenarioDemandUseCase must not be null");
        this.simulationResultUseCase = Objects.requireNonNull(simulationResultUseCase, "GetScenarioSimulationResultUseCase must not be null");
    }

    @Override
    @Transactional
    public ScenarioResult createScenario(CreateScenarioCommand command) {
        return createScenarioUseCase.createScenario(command);
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioDetailResult getScenarioById(Long scenarioId) {
        return getScenarioUseCase.getScenarioById(scenarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScenarioResult> listScenarios(Long orgUnitId) {
        return listScenariosUseCase.listScenarios(orgUnitId);
    }

    @Override
    @Transactional
    public ScenarioDemandResult addDemand(AddScenarioDemandCommand command) {
        return addDemandUseCase.addDemand(command);
    }

    @Override
    @Transactional
    public ScenarioDemandResult updateDemand(UpdateScenarioDemandCommand command) {
        return updateDemandUseCase.updateDemand(command);
    }

    @Override
    @Transactional
    public void deleteDemand(Long scenarioId, Long demandId) {
        deleteDemandUseCase.deleteDemand(scenarioId, demandId);
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioSimulationResult getSimulationResult(Long scenarioId) {
        return simulationResultUseCase.getSimulationResult(scenarioId);
    }
}
