package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;

public class OrgUnitPersistenceMapper {
    public static OrgUnit toDomain(OrgUnitJpaEntity entity) {
        if (entity == null)
            return null;
        return new OrgUnit(
                entity.getId() != null ? new OrgUnitId(entity.getId()) : null,
                entity.getUnitCode(),
                entity.getUnitName(),
                entity.getUnitType(),
                entity.getParentId() != null ? new OrgUnitId(entity.getParentId()) : null,
                entity.getTreePath(),
                entity.getLevel(),
                entity.getStatus(),
                entity.getDescription(),
                null, // managerId
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static OrgUnitJpaEntity toJpaEntity(OrgUnit domain) {
        if (domain == null)
            return null;
        return new OrgUnitJpaEntity(
                domain.getId() != null ? domain.getId().getValue() : null,
                domain.getUnitCode(),
                domain.getUnitName(),
                domain.getUnitType(),
                domain.getParentId() != null ? domain.getParentId().getValue() : null,
                domain.getTreePath(),
                domain.getLevel(),
                domain.getStatus(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}