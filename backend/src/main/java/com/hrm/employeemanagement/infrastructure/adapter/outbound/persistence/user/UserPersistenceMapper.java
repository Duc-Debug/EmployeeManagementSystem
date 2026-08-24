package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;

/**
 * Dedicated Mapper between Domain Models and JPA Entities.
 *
 * Keeps Domain independent from JPA and preserves @Version
 * across the persistence lifecycle.
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity, Long employeeId) {
        if (entity == null) return null;
        Role role = toDomain(entity.getRole());
        UserStatus status = Boolean.TRUE.equals(entity.getIsActive()) ? UserStatus.ACTIVE : UserStatus.LOCKED;
        UserId userId = entity.getId() != null ? new UserId(entity.getId()) : null;
        EmployeeId empId = employeeId != null ? new EmployeeId(employeeId) : null;
        return new User(userId, entity.getUsername(), entity.getPasswordHash(), role, status, empId, entity.getEmail(), entity.getPasswordChangedAt(), entity.getTokenVersion(), entity.getVersion());
    }

    public UserJpaEntity toJpaEntity(User domain, RoleJpaEntity roleJpa) {
        if (domain == null) return null;
        UserJpaEntity entity = new UserJpaEntity(
                domain.getIdValue(),
                domain.getUsername(),
                domain.getPasswordHash(),
                roleJpa,
                domain.isActive(),
                domain.getEmail(),
                domain.getPasswordChangedAt(),
                domain.getTokenVersion(),
                domain.getVersion()
        );
        return entity;
    }

    public void updateJpaEntity(UserJpaEntity target, User domain, RoleJpaEntity roleJpa) {
        if (target == null || domain == null) return;
        target.setUsername(domain.getUsername());
        target.setPasswordHash(domain.getPasswordHash());
        target.setRole(roleJpa);
        target.setIsActive(domain.isActive());
        target.setEmail(domain.getEmail());
        target.setPasswordChangedAt(domain.getPasswordChangedAt());
        target.setTokenVersion(domain.getTokenVersion());
        if (domain.getVersion() != null) {
            target.setVersion(
                    domain.getVersion()
            );
        }
    }

    public Role toDomain(
            RoleJpaEntity entity
    ) {
        if (entity == null) {
            return null;
        }

        RoleId roleId =
                entity.getId() != null
                        ? new RoleId(entity.getId())
                        : null;

        return new Role(
                roleId,
                RoleCode.fromCode(
                        entity.getCode()
                ),
                entity.getName()
        );
    }

    public RoleJpaEntity toJpaEntity(
            Role domain
    ) {
        if (domain == null) {
            return null;
        }

        return new RoleJpaEntity(
                domain.getIdValue(),
                domain.getCode().getCode(),
                domain.getName()
        );
    }

    public Employee toDomain(
            EmployeeJpaEntity entity
    ) {
        if (entity == null) {
            return null;
        }

        EmployeeId employeeId =
                entity.getId() != null
                        ? new EmployeeId(entity.getId())
                        : null;

        UserId userId =
                entity.getUserId() != null
                        ? new UserId(entity.getUserId())
                        : null;

        EmployeeStatus status =
                EmployeeStatus.fromString(
                        entity.getStatus()
                );

        return new Employee(
                employeeId,
                userId,
                entity.getOrgUnitId(),
                entity.getEmployeeCode(),
                entity.getFullName(),
                entity.getIsOutsourced(),
                entity.getStandardHoursPerWeek(),
                status
        );
    }

    public EmployeeJpaEntity toJpaEntity(
            Employee domain
    ) {
        if (domain == null) {
            return null;
        }

        return new EmployeeJpaEntity(
                domain.getIdValue(),
                domain.getUserIdValue(),
                domain.getOrgUnitId(),
                domain.getEmployeeCode(),
                domain.getFullName(),
                domain.getIsOutsourced(),
                domain.getStandardHoursPerWeek(),
                domain.getStatusValue()
        );
    }

    public void updateJpaEntity(
            EmployeeJpaEntity target,
            Employee domain
    ) {
        if (target == null || domain == null) {
            return;
        }

        target.setOrgUnitId(
                domain.getOrgUnitId()
        );

        target.setEmployeeCode(
                domain.getEmployeeCode()
        );

        target.setFullName(
                domain.getFullName()
        );

        target.setIsOutsourced(
                domain.getIsOutsourced()
        );

        target.setStandardHoursPerWeek(
                domain.getStandardHoursPerWeek()
        );

        target.setStatus(
                domain.getStatusValue()
        );
    }

    public AuditLog toDomain(
            AuditLogJpaEntity entity
    ) {
        if (entity == null) {
            return null;
        }

        return new AuditLog(
                entity.getId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getTableName(),
                entity.getRecordId(),
                entity.getCreatedAt(),
                entity.getOldValue(),
                entity.getNewValue()
        );
    }

    public AuditLogJpaEntity toJpaEntity(
            AuditLog domain
    ) {
        if (domain == null) {
            return null;
        }

        return new AuditLogJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getAction(),
                domain.getTableName(),
                domain.getRecordId(),
                domain.getCreatedAt(),
                domain.getOldValue(),
                domain.getNewValue()
        );
    }
}
