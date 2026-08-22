package com.hrm.employeemanagement.application.dto.user;

public record UpdateUserRoleCommand(
        Long userId,
        String roleCode,
        Long departmentId
) {
    public UpdateUserRoleCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được để trống");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Mã vai trò không được để trống");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("ID phòng ban không được để trống");
        }
    }
}
