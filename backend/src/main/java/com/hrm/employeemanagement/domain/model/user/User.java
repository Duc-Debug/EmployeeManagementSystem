package com.hrm.employeemanagement.domain.model.user;

import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;

import java.util.Objects;

public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private Long employeeId;

    public User(Long id, String username, String passwordHash, Role role, UserStatus status, Long employeeId) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username không được null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "PasswordHash không được null");
        this.role = Objects.requireNonNull(role, "Role không được null");
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.employeeId = employeeId;
    }

    public static User createNew(String username, String passwordHash, Role role, Long employeeId) {
        return new User(null, username, passwordHash, role, UserStatus.ACTIVE, employeeId);
    }

    public void lock(Long currentAdminId, long activeAdminCount) {
        if (this.id != null && this.id.equals(currentAdminId)) {
            throw new SelfLockingException("Bạn không thể tự khóa tài khoản của chính mình");
        }
        if (isSystemAdmin() && activeAdminCount <= 1) {
            throw new LastAdminProtectionException("Không thể khóa tài khoản Quản trị viên duy nhất của hệ thống");
        }
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        this.status = UserStatus.ACTIVE;
    }

    public void changeRole(Role newRole, long activeAdminCount) {
        if (isSystemAdmin() && newRole.getCode() != RoleCode.VT_06 && activeAdminCount <= 1) {
            throw new LastAdminProtectionException("Không thể hạ quyền Quản trị viên duy nhất của hệ thống");
        }
        this.role = Objects.requireNonNull(newRole, "Role mới không được null");
    }

    public boolean isSystemAdmin() {
        return this.role != null && this.role.getCode() == RoleCode.VT_06;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
}
