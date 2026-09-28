package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.DeleteSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.SaveSimulatedEmployeePort;
import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;
import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.entity.ScenarioSimulatedEmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.mapper.ScenarioSimulatedEmployeePersistenceMapper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.repository.SpringDataScenarioSimulatedEmployeeRepository;

@Component
public class ScenarioSimulatedEmployeePersistenceAdapter implements
        SaveSimulatedEmployeePort,
        LoadSimulatedEmployeePort,
        DeleteSimulatedEmployeePort {

    private final SpringDataScenarioSimulatedEmployeeRepository repository;

    public ScenarioSimulatedEmployeePersistenceAdapter(SpringDataScenarioSimulatedEmployeeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataScenarioSimulatedEmployeeRepository must not be null");
    }

    @Override
    public ScenarioSimulatedEmployee save(ScenarioSimulatedEmployee employee) {
        ScenarioSimulatedEmployeeJpaEntity entity = ScenarioSimulatedEmployeePersistenceMapper.toEntity(employee);
        ScenarioSimulatedEmployeeJpaEntity saved = repository.save(entity);
        return ScenarioSimulatedEmployeePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<ScenarioSimulatedEmployee> findById(SimulatedEmployeeId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return repository.findById(id.value()).map(ScenarioSimulatedEmployeePersistenceMapper::toDomain);
    }

    @Override
    public List<ScenarioSimulatedEmployee> findByScenarioId(Long scenarioId) {
        if (scenarioId == null) {
            return List.of();
        }
        return repository.findByScenarioId(scenarioId).stream()
                .map(ScenarioSimulatedEmployeePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(SimulatedEmployeeId id) {
        if (id != null && id.value() != null) {
            repository.deleteById(id.value());
        }
    }
}
