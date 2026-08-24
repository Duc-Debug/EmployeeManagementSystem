package com.hrm.employeemanagement.application.dto.user;

import com.hrm.employeemanagement.domain.authorization.DataScope;

public record UpdateUserRoleCommand(
        Long userId,
        String roleCode,
        Long orgUnitId,
        DataScope dataScope,
        Long scopeOrgUnitId
) {
    public UpdateUserRoleCommand(
            Long userId,
            String roleCode,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        this(
                userId,
                roleCode,
                null,
                dataScope,
                scopeOrgUnitId
        );
    }
}
