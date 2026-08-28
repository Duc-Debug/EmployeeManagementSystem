package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

import com.hrm.employeemanagement.domain.authorization.DataScope;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String employeeCode;

    private Long orgUnitId;

    @NotBlank(message = "Mã vai trò không được để trống")
    private String roleCode;

    private DataScope dataScope;

    private Long scopeOrgUnitId;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
    }

    public Long getScopeOrgUnitId() {
        return scopeOrgUnitId;
    }

    public void setScopeOrgUnitId(Long scopeOrgUnitId) {
        this.scopeOrgUnitId = scopeOrgUnitId;
    }
}
