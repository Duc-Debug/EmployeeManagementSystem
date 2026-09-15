package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioDemandPort;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository.SpringDataScenarioDemandRepository;

@Component
public class ScenarioDemandPersistenceAdapter implements
        SaveScenarioDemandPort,
        LoadScenarioDemandPort,
        DeleteScenarioDemandPort {

    private final SpringDataScenarioDemandRepository repository;

    public ScenarioDemandPersistenceAdapter(SpringDataScenarioDemandRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataScenarioDemandRepository must not be null");
    }

    @Override
    public ScenarioDemand save(ScenarioDemand demand) {
        ScenarioDemandJpaEntity entity = toEntity(demand);
        ScenarioDemandJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ScenarioDemand> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ScenarioDemand> findByScenarioId(Long scenarioId) {
        return repository.findByScenarioIdOrderByStartYearAscStartWeekAsc(scenarioId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long demandId) {
        repository.deleteById(demandId);
    }

    @Override
    public void deleteAllByScenarioId(Long scenarioId) {
        repository.deleteByScenarioId(scenarioId);
    }

    private ScenarioDemandJpaEntity toEntity(ScenarioDemand domain) {
        return new ScenarioDemandJpaEntity(
                domain.getId(),
                domain.getScenarioId(),
                domain.getDemandName(),
                domain.getHeadcount(),
                domain.getStartYear(),
                domain.getStartWeek(),
                domain.getEndYear(),
                domain.getEndWeek(),
                domain.getHoursPerWeekPerPerson(),
                domain.getSkillRequirement(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private ScenarioDemand toDomain(ScenarioDemandJpaEntity entity) {
        return new ScenarioDemand(
                entity.getId(),
                entity.getScenarioId(),
                entity.getDemandName(),
                entity.getHeadcount(),
                entity.getStartYear(),
                entity.getStartWeek(),
                entity.getEndYear(),
                entity.getEndWeek(),
                entity.getHoursPerWeekPerPerson(),
                entity.getSkillRequirement(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
