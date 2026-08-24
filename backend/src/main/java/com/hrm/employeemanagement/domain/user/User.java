package com.hrm.employeemanagement.domain.user;

import java.util.Objects;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;

/**
 * Rich Domain Aggregate Root for User.
 * Invariants and business logic are fully encapsulated.
 */
public class User {
    private UserId id;
    private String username;
    private String passwordHash;
    private Role role;
    private DataScope dataScope;
    private Long scopeOrgUnitId;
    private UserStatus status;
    private EmployeeId employeeId;
    private Long version;

    public User(
        UserId id,
        String username,
        String passwordHash,
        Role role,
        UserStatus status,
        EmployeeId employeeId
) {
    this(
            id,
            username,
            passwordHash,
            role,
            status,
            employeeId,
            defaultDataScopeFor(role),
            null,
            null
    );
}

   public User(
        UserId id,
        String username,
        String passwordHash,
        Role role,
        UserStatus status,
        EmployeeId employeeId,
        DataScope dataScope,
        Long scopeOrgUnitId,
        Long version
) {
    this.id = id;
    this.username = Objects.requireNonNull(
            username,
            "Username không được null"
    );
    this.passwordHash = Objects.requireNonNull(
            passwordHash,
            "PasswordHash không được null"
    );
    this.role = Objects.requireNonNull(
            role,
            "Role không được null"
    );

    this.status = status != null
            ? status
            : UserStatus.ACTIVE;

    this.employeeId = employeeId;

    validateDataScope(
            dataScope,
            scopeOrgUnitId
    );

    validateRoleDataScope(
            role,
            dataScope,
            scopeOrgUnitId
    );

    this.dataScope = dataScope;
    this.scopeOrgUnitId = scopeOrgUnitId;

    this.version = version;
}

    public static User createNew(
        String username,
        String passwordHash,
        Role role,
        EmployeeId employeeId
) {
    return new User(
            null,
            username,
            passwordHash,
            role,
            UserStatus.ACTIVE,
            employeeId,
            defaultDataScopeFor(role),
            null,
            null
    );
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

        Role targetRole =
                Objects.requireNonNull(
                        newRole,
                        "Role mới không được null"
                );

        validateRoleDataScope(
                targetRole,
                dataScope,
                scopeOrgUnitId
        );

        this.role = targetRole;
    }

    public void changeAuthorization(
            Role newRole,
            DataScope dataScope,
            Long scopeOrgUnitId,
            long activeAdminCount
    ) {
        Role targetRole =
                Objects.requireNonNull(
                        newRole,
                        "Role mới không được null"
                );

        validateDataScope(
                dataScope,
                scopeOrgUnitId
        );

        validateRoleDataScope(
                targetRole,
                dataScope,
                scopeOrgUnitId
        );

        if (isSystemAdmin()
                && targetRole.getCode() != RoleCode.VT_06
                && activeAdminCount <= 1) {
            throw new LastAdminProtectionException(
                    "Không thể hạ quyền Quản trị viên duy nhất của hệ thống"
            );
        }

        this.role = targetRole;
        this.dataScope = dataScope;
        this.scopeOrgUnitId = scopeOrgUnitId;
    }

    public void changeDataScope(
        DataScope dataScope,
        Long scopeOrgUnitId
) {
    validateDataScope(
            dataScope,
            scopeOrgUnitId
    );

    validateRoleDataScope(
            role,
            dataScope,
            scopeOrgUnitId
    );

    this.dataScope = dataScope;
    this.scopeOrgUnitId = scopeOrgUnitId;
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
    public DataScope getDataScope() {
    return dataScope;
    }

    public Long getScopeOrgUnitId() {
    return scopeOrgUnitId;
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

    public Long getVersion() {
        return version;
    }

    public User linkEmployee(EmployeeId employeeId) {
        this.employeeId = Objects.requireNonNull(employeeId, "EmployeeId không được null");
        return this;
    }

    public void setId(UserId id) {
        this.id = id;
    }

    public void setEmployeeId(EmployeeId employeeId) {
        linkEmployee(employeeId);
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    private void validateDataScope(
        DataScope dataScope,
        Long scopeOrgUnitId
) {
    Objects.requireNonNull(
            dataScope,
            "DataScope must not be null"
    );

    if (dataScope == DataScope.ORGANIZATION_BRANCH
            && scopeOrgUnitId == null) {

        throw new IllegalArgumentException(
                "ORGANIZATION_BRANCH requires scopeOrgUnitId"
        );
    }

    if (dataScope != DataScope.ORGANIZATION_BRANCH
            && scopeOrgUnitId != null) {

        throw new IllegalArgumentException(
                "scopeOrgUnitId is only allowed for ORGANIZATION_BRANCH"
            );
    }
}

private static DataScope defaultDataScopeFor(Role role) {
    Role requiredRole =
            Objects.requireNonNull(
                    role,
                    "Role không được null"
            );

    return requiredRole.isSystemAdmin()
            ? DataScope.COMPANY
            : DataScope.SELF;
}

private void validateRoleDataScope(
        Role role,
        DataScope dataScope,
        Long scopeOrgUnitId
) {
    if (role != null
            && role.isSystemAdmin()
            && (dataScope != DataScope.COMPANY
            || scopeOrgUnitId != null)) {
        throw new IllegalArgumentException(
                "SYSTEM_ADMIN requires COMPANY data scope"
        );
    }
}
}
