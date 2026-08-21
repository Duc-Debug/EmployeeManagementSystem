package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import java.util.List;

public record OrgUnitNodeResult(
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
        List<OrgUnitNodeResult> children) {
}
