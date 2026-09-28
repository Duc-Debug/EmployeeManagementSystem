package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import java.time.LocalDateTime;

public record OrgUnitResult(
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
}
