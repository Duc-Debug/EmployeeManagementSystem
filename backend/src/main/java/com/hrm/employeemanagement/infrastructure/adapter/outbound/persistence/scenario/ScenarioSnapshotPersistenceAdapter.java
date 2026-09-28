package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSnapshotPort;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioAllocationSnapshotJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository.SpringDataScenarioSnapshotRepository;

@Component
public class ScenarioSnapshotPersistenceAdapter implements
        SaveScenarioSnapshotPort,
        LoadScenarioSnapshotPort,
        DeleteScenarioSnapshotPort {

    private final SpringDataScenarioSnapshotRepository repository;

    public ScenarioSnapshotPersistenceAdapter(SpringDataScenarioSnapshotRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataScenarioSnapshotRepository must not be null");
    }

    @Override
    public void deleteByScenarioId(Long scenarioId) {
        if (scenarioId != null) {
            repository.deleteByScenarioId(scenarioId);
        }
    }

    @Override
    public void saveAll(List<ScenarioAllocationSnapshotItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<ScenarioAllocationSnapshotJpaEntity> entities = items.stream()
                .map(this::toEntity)
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public List<ScenarioAllocationSnapshotItem> findByScenarioId(Long scenarioId) {
        return repository.findByScenarioId(scenarioId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ScenarioAllocationSnapshotJpaEntity toEntity(ScenarioAllocationSnapshotItem domain) {
        return new ScenarioAllocationSnapshotJpaEntity(
                domain.getId(),
                domain.getScenarioId(),
                domain.getEmployeeId(),
                domain.getYearNumber(),
                domain.getWeekNumber(),
                domain.getAllocatedHours(),
                domain.getAvailableHours()
        );
    }

    private ScenarioAllocationSnapshotItem toDomain(ScenarioAllocationSnapshotJpaEntity entity) {
        return new ScenarioAllocationSnapshotItem(
                entity.getId(),
                entity.getScenarioId(),
                entity.getEmployeeId(),
                entity.getYearNumber(),
                entity.getWeekNumber(),
                entity.getAllocatedHours(),
                entity.getAvailableHours()
        );
    }
}
