package com.hrm.employeemanagement.infrastructure.adapter.in.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRoleRequest {

    @NotBlank(message = "Mã vai trò không được để trống")
    private String roleCode;

    @NotNull(message = "ID bộ phận không được để trống")
    private Long departmentId;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
