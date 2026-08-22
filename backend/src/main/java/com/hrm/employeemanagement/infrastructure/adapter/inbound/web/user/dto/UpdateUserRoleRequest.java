package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRoleRequest {

    @NotBlank(message = "Mã vai trò không được để trống")
    private String roleCode;

    @NotNull(message = "ID đơn vị tổ chức không được để trống")
    private Long orgUnitId;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }
}
