package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulationScenarioPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.repository.SpringDataSimulationScenarioRepository;

@Component
public class SimulationScenarioPersistenceAdapter implements LoadSimulationScenarioPort {

    private final SpringDataSimulationScenarioRepository repository;

    public SimulationScenarioPersistenceAdapter(SpringDataSimulationScenarioRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataSimulationScenarioRepository must not be null");
    }

    @Override
    public boolean existsById(Long scenarioId) {
        if (scenarioId == null) {
            return false;
        }
        return repository.existsById(scenarioId);
    }

    @Override
    public Optional<SimulationScenarioInfo> findById(Long scenarioId) {
        if (scenarioId == null) {
            return Optional.empty();
        }
        return repository.findById(scenarioId)
                .map(e -> new SimulationScenarioInfo(e.getId(), e.getScenarioCode(), e.getName(), e.getStatus()));
    }
}
