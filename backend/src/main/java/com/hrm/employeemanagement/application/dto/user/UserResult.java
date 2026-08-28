package com.hrm.employeemanagement.application.dto.user;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.user.UserStatus;

public class UserResult {
    private final Long id;
    private final String username;
    private final String email;
    private final String roleCode;
    private final String roleName;
    private final UserStatus status;
    private final Long employeeId;
    private final String fullName;
    private final Long orgUnitId;
    private final String orgUnitName;
    private final DataScope dataScope;
    private final Long scopeOrgUnitId;

    public UserResult(
            Long id,
            String username,
            String email,
            String roleCode,
            String roleName,
            UserStatus status,
            Long employeeId,
            String fullName,
            Long orgUnitId,
            String orgUnitName,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.status = status;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.orgUnitId = orgUnitId;
        this.orgUnitName = orgUnitName;
        this.dataScope = dataScope;
        this.scopeOrgUnitId = scopeOrgUnitId;
    }

    public UserResult(
            Long id,
            String username,
            String roleCode,
            String roleName,
            UserStatus status,
            Long employeeId,
            String fullName,
            Long orgUnitId,
            String orgUnitName,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        this(id, username, null, roleCode, roleName, status, employeeId, fullName, orgUnitId, orgUnitName, dataScope, scopeOrgUnitId);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public Long getScopeOrgUnitId() {
        return scopeOrgUnitId;
    }
}
