package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSharePort;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioShareJpaEntity;

@Component
public class JpaScenarioShareRepositoryAdapter implements SaveScenarioSharePort, LoadScenarioSharePort {

    private final SpringDataScenarioShareRepository repository;

    public JpaScenarioShareRepositoryAdapter(SpringDataScenarioShareRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataScenarioShareRepository must not be null");
    }

    @Override
    public ScenarioShare save(ScenarioShare share) {
        ScenarioShareJpaEntity entity = toEntity(share);
        ScenarioShareJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ScenarioShare> saveAll(List<ScenarioShare> shares) {
        if (shares == null || shares.isEmpty()) {
            return List.of();
        }
        List<ScenarioShareJpaEntity> entities = shares.stream().map(this::toEntity).toList();
        List<ScenarioShareJpaEntity> saved = repository.saveAll(entities);
        return saved.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ScenarioShare> findActiveShare(Long scenarioId, Long userId) {
        return repository.findActiveShare(scenarioId, userId).map(this::toDomain);
    }

    @Override
    public List<ScenarioShare> findActiveSharesByScenarioId(Long scenarioId) {
        return repository.findActiveSharesByScenarioId(scenarioId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ScenarioShare> findActiveSharesByUserId(Long userId) {
        return repository.findActiveSharesByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean hasActiveShare(Long scenarioId, Long userId) {
        return repository.hasActiveShare(scenarioId, userId);
    }

    private ScenarioShareJpaEntity toEntity(ScenarioShare domain) {
        return new ScenarioShareJpaEntity(
                domain.getId(),
                domain.getScenarioId(),
                domain.getSharedWithUserId(),
                domain.getSharedByUserId(),
                domain.getAccessLevel(),
                domain.getCreatedAt(),
                domain.getRevokedAt(),
                domain.getVersion()
        );
    }

    private ScenarioShare toDomain(ScenarioShareJpaEntity entity) {
        return new ScenarioShare(
                entity.getId(),
                entity.getScenarioId(),
                entity.getSharedWithUserId(),
                entity.getSharedByUserId(),
                entity.getAccessLevel(),
                entity.getCreatedAt(),
                entity.getRevokedAt(),
                entity.getVersion()
        );
    }
}
