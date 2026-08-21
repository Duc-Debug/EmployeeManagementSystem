package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

public record CreateOrgUnitCommand(
    String unitCode,
    String unitName,
    OrgUnitType unitType,
    Long parentId,
    String description
) {}
