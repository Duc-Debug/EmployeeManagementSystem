package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
public record UpdateOrgUnitCommand(
    Long id,
    String unitName,
    OrgUnitType unitType,
    Long managerId,
    String description
) {}
