package com.hrm.employeemanagement.domain.user;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;

import java.util.Objects;

/**
 * Rich Domain Aggregate Root for User.
 * Invariants and business logic are fully encapsulated.
 */
public class User {
    private UserId id;
    private String username;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private EmployeeId employeeId;

    public User(UserId id, String username, String passwordHash, Role role, UserStatus status, EmployeeId employeeId) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username không được null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "PasswordHash không được null");
        this.role = Objects.requireNonNull(role, "Role không được null");
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.employeeId = employeeId;
    }

    public static User createNew(String username, String passwordHash, Role role, EmployeeId employeeId) {
        return new User(null, username, passwordHash, role, UserStatus.ACTIVE, employeeId);
    }

    public void lock(UserId currentAdminId, long activeAdminCount) {
        if (this.status == UserStatus.LOCKED) {
            throw new com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException("Tài khoản này hiện đã bị khóa");
        }
        if (this.id != null && currentAdminId != null && this.id.equals(currentAdminId)) {
            throw new SelfLockingException("Bạn không thể tự khóa tài khoản của chính mình");
        }
        if (isSystemAdmin() && activeAdminCount <= 1) {
            throw new LastAdminProtectionException("Không thể khóa tài khoản Quản trị viên duy nhất của hệ thống");
        }
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        if (this.status == UserStatus.ACTIVE) {
            throw new com.hrm.employeemanagement.domain.exception.user.UserAlreadyActiveException("Tài khoản này hiện đang ở trạng thái hoạt động");
        }
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

    public UserId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
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

    public EmployeeId getEmployeeId() {
        return employeeId;
    }

    public Long getEmployeeIdValue() {
        return employeeId != null ? employeeId.value() : null;
    }

    public void setId(UserId id) {
        this.id = id;
    }

    public void setEmployeeId(EmployeeId employeeId) {
        this.employeeId = employeeId;
    }
}
