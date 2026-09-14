package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.SaveCapacityThresholdPort;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.entity.CapacityThresholdConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.repository.SpringDataCapacityThresholdRepository;

@Component
public class CapacityThresholdPersistenceAdapter implements LoadCapacityThresholdPort, SaveCapacityThresholdPort {

    private final SpringDataCapacityThresholdRepository repository;
    private final CapacityThresholdPersistenceMapper mapper;

    public CapacityThresholdPersistenceAdapter(
            SpringDataCapacityThresholdRepository repository,
            CapacityThresholdPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<CapacityThresholdConfig> findByScope(CapacityThresholdScope scopeType, Long orgUnitId) {
        return repository.findByScopeTypeAndOrgUnitId(scopeType, orgUnitId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CapacityThresholdConfig> findByScopeKey(String scopeKey) {
        return repository.findByScopeKey(scopeKey)
                .map(mapper::toDomain);
    }

    @Override
    public CapacityThresholdConfig save(CapacityThresholdConfig config) {
        CapacityThresholdConfigJpaEntity jpaEntity;
        if (config.getId() != null) {
            jpaEntity = repository.findById(config.getId())
                    .orElseGet(() -> mapper.toJpaEntity(config));
            mapper.updateJpaEntity(jpaEntity, config);
        } else {
            jpaEntity = mapper.toJpaEntity(config);
        }

        CapacityThresholdConfigJpaEntity saved = repository.save(jpaEntity);
        return mapper.toDomain(saved);
    }
}
