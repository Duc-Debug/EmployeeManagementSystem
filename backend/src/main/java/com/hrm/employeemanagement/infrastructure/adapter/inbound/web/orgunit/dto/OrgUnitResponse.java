package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import java.time.LocalDateTime;

public record OrgUnitResponse(
        Long id,
        String unitCode,
        String unitName,
        OrgUnitType unitType,
        Long parentId,
        String treePath,
        Integer level,
        OrgUnitStatus status,
        String description,
        Long managerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static OrgUnitResponse fromResult(OrgUnitResult result) {
        return new OrgUnitResponse(
                result.id(),
                result.unitCode(),
                result.unitName(),
                result.unitType(),
                result.parentId(),
                result.treePath(),
                result.level(),
                result.status(),
                result.description(),
                result.managerId(),
                result.createdAt(),
                result.updatedAt());
    }
}