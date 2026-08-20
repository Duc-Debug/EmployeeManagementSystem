package com.hrm.employeemanagement.application.dto.user;

import com.hrm.employeemanagement.domain.model.user.UserStatus;

public class UserResult {
    private final Long id;
    private final String username;
    private final String roleCode;
    private final String roleName;
    private final UserStatus status;
    private final Long employeeId;
    private final String fullName;
    private final Long departmentId;
    private final String departmentName;

    public UserResult(Long id, String username, String roleCode, String roleName, UserStatus status, Long employeeId, String fullName, Long departmentId, String departmentName) {
        this.id = id;
        this.username = username;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.status = status;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
