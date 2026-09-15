package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveResourceScenarioPort;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ResourceScenarioJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.repository.SpringDataResourceScenarioRepository;

@Component
public class ResourceScenarioPersistenceAdapter implements SaveResourceScenarioPort, LoadResourceScenarioPort {

    private final SpringDataResourceScenarioRepository repository;

    public ResourceScenarioPersistenceAdapter(SpringDataResourceScenarioRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataResourceScenarioRepository must not be null");
    }

    @Override
    public ResourceScenario save(ResourceScenario scenario) {
        ResourceScenarioJpaEntity entity = toEntity(scenario);
        ResourceScenarioJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ResourceScenario> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ResourceScenario> findByCode(String code) {
        return repository.findByCode(code).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public List<ResourceScenario> findAllByOrgUnitIds(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return List.of();
        }
        return repository.findByOrgUnitIdInOrderByCreatedAtDesc(orgUnitIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ResourceScenario> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    private ResourceScenarioJpaEntity toEntity(ResourceScenario domain) {
        return new ResourceScenarioJpaEntity(
                domain.getId(),
                domain.getCode(),
                domain.getName(),
                domain.getDescription(),
                domain.getOrgUnitId(),
                domain.getStatus().getValue(),
                domain.getFromYear(),
                domain.getFromWeek(),
                domain.getDurationWeeks(),
                domain.getBaseSnapshotAt(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }

    private ResourceScenario toDomain(ResourceScenarioJpaEntity entity) {
        return new ResourceScenario(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getOrgUnitId(),
                ScenarioStatus.fromString(entity.getStatus()),
                entity.getFromYear(),
                entity.getFromWeek(),
                entity.getDurationWeeks(),
                entity.getBaseSnapshotAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
