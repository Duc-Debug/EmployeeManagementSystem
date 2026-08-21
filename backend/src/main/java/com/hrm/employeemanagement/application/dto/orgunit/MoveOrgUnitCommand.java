package com.hrm.employeemanagement.application.dto.orgunit;

public record MoveOrgUnitCommand(
    Long id,
    Long newParentId
) {}
