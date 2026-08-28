package com.hrm.employeemanagement.application.dto.user;

public record CreateUserCommand(
        String username,
        String password,
        String roleCode,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String email
) {
    public CreateUserCommand(
            String username,
            String password,
            String roleCode,
            String employeeCode,
            String fullName,
            Long orgUnitId
    ) {
        this(username, password, roleCode, employeeCode, fullName, orgUnitId, null);
    }

    public CreateUserCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Mã vai trò không được để trống");
        }
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException("Mã nhân viên không được để trống");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        if (orgUnitId == null) {
            throw new IllegalArgumentException("ID đơn vị tổ chức không được để trống");
        }
    }
}
