package com.hrm.employeemanagement.application.dto.orgunit;

public record OrgUnitMemberResult(
        Long id,
        String employeeCode,
        String fullName,
        String professionalRole
) {}
