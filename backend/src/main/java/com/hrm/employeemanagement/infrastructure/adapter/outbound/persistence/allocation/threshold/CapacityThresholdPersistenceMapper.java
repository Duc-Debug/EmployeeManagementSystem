package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.entity.CapacityThresholdConfigJpaEntity;

@Component
public class CapacityThresholdPersistenceMapper {

    public CapacityThresholdConfig toDomain(CapacityThresholdConfigJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CapacityThresholdConfig(
                entity.getId(),
                entity.getScopeType(),
                entity.getScopeKey(),
                entity.getOrgUnitId(),
                entity.getOverloadThreshold(),
                entity.getIdleThreshold(),
                entity.getVersion(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public CapacityThresholdConfigJpaEntity toJpaEntity(CapacityThresholdConfig domain) {
        if (domain == null) {
            return null;
        }

        return new CapacityThresholdConfigJpaEntity(
                domain.getId(),
                domain.getScopeType(),
                domain.getScopeKey(),
                domain.getOrgUnitId(),
                domain.getOverloadThreshold(),
                domain.getIdleThreshold(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedBy(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }

    public void updateJpaEntity(CapacityThresholdConfigJpaEntity entity, CapacityThresholdConfig domain) {
        entity.setOverloadThreshold(domain.getOverloadThreshold());
        entity.setIdleThreshold(domain.getIdleThreshold());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
    }
}
