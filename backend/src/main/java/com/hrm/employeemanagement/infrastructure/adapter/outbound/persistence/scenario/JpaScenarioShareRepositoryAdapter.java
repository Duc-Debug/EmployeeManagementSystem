package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSharePort;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioShareJpaEntity;

import org.springframework.dao.DataIntegrityViolationException;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioShareException;

@Component
public class JpaScenarioShareRepositoryAdapter implements SaveScenarioSharePort, LoadScenarioSharePort {

    private final SpringDataScenarioShareRepository repository;

    public JpaScenarioShareRepositoryAdapter(SpringDataScenarioShareRepository repository) {
        this.repository = Objects.requireNonNull(repository, "SpringDataScenarioShareRepository must not be null");
    }

    @Override
    public ScenarioShare save(ScenarioShare share) {
        try {
            ScenarioShareJpaEntity entity = toEntity(share);
            ScenarioShareJpaEntity saved = repository.saveAndFlush(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueShareViolation(ex)) {
                throw new DuplicateScenarioShareException("Kịch bản đã được chia sẻ cho người dùng này");
            }
            throw ex;
        }
    }

    @Override
    public List<ScenarioShare> saveAll(List<ScenarioShare> shares) {
        if (shares == null || shares.isEmpty()) {
            return List.of();
        }
        try {
            List<ScenarioShareJpaEntity> entities = shares.stream().map(this::toEntity).toList();
            List<ScenarioShareJpaEntity> saved = repository.saveAllAndFlush(entities);
            return saved.stream().map(this::toDomain).toList();
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueShareViolation(ex)) {
                throw new DuplicateScenarioShareException("Kịch bản đã được chia sẻ cho người dùng trong danh sách này");
            }
            throw ex;
        }
    }

    private boolean isUniqueShareViolation(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null && (constraintName.toLowerCase().contains("uk_scenario_active_share")
                        || constraintName.toLowerCase().contains("scenario_shares"))) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && (message.toLowerCase().contains("uk_scenario_active_share")
                    || message.toLowerCase().contains("duplicate entry"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
