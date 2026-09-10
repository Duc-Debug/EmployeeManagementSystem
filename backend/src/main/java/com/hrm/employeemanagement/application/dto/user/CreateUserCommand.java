package com.hrm.employeemanagement.application.dto.user;

public record CreateUserCommand(
        String username,
        String password,
        String roleCode,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String email,
        Long scopeOrgUnitId
) {
    public CreateUserCommand(
            String username,
            String password,
            String roleCode,
            String employeeCode,
            String fullName,
            Long orgUnitId
    ) {
        this(username, password, roleCode, employeeCode, fullName, orgUnitId, null, null);
    }

    public CreateUserCommand(
            String username,
            String password,
            String roleCode,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String email
    ) {
        this(username, password, roleCode, employeeCode, fullName, orgUnitId, email, null);
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
        if (email != null && !email.isBlank()) {
            String trimmed = email.trim();
            if (!trimmed.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalArgumentException("Email không đúng định dạng (phải có ký tự '@' và tên miền có dấu '.' hợp lệ, ví dụ: user@company.com)");
            }
        }
    }
}
