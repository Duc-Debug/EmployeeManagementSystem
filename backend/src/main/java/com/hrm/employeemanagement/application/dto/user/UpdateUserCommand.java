package com.hrm.employeemanagement.application.dto.user;

import com.hrm.employeemanagement.domain.authorization.DataScope;

public record UpdateUserCommand(
        Long userId,
        String fullName,
        String email,
        String employeeCode,
        Long orgUnitId,
        String roleCode,
        DataScope dataScope,
        Long scopeOrgUnitId
) {
    public UpdateUserCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được để trống");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Mã vai trò không được để trống");
        }
    }
}
