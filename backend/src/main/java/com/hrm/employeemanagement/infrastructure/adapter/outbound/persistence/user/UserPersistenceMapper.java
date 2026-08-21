package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
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
import org.springframework.stereotype.Component;

/**
 * Dedicated Mapper between Domain Models and JPA Entities.
 * Enforces clean separation of concerns and prevents inline mapping bloat in Adapters.
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity, Long employeeId) {
        if (entity == null) return null;
        Role role = toDomain(entity.getRole());
        UserStatus status = Boolean.TRUE.equals(entity.getIsActive()) ? UserStatus.ACTIVE : UserStatus.LOCKED;
        UserId userId = entity.getId() != null ? new UserId(entity.getId()) : null;
        EmployeeId empId = employeeId != null ? new EmployeeId(employeeId) : null;
        return new User(userId, entity.getUsername(), entity.getPasswordHash(), role, status, empId);
    }

    public UserJpaEntity toJpaEntity(User domain, RoleJpaEntity roleJpa) {
        if (domain == null) return null;
        return new UserJpaEntity(
                domain.getIdValue(),
                domain.getUsername(),
                domain.getPasswordHash(),
                roleJpa,
                domain.isActive()
        );
    }

    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) return null;
        RoleId roleId = entity.getId() != null ? new RoleId(entity.getId()) : null;
        return new Role(roleId, RoleCode.fromCode(entity.getCode()), entity.getName());
    }

    public RoleJpaEntity toJpaEntity(Role domain) {
        if (domain == null) return null;
        return new RoleJpaEntity(domain.getIdValue(), domain.getCode().getCode(), domain.getName());
    }

    public Employee toDomain(EmployeeJpaEntity entity) {
        if (entity == null) return null;
        EmployeeId empId = entity.getId() != null ? new EmployeeId(entity.getId()) : null;
        UserId userId = entity.getUserId() != null ? new UserId(entity.getUserId()) : null;
        return new Employee(
                empId,
                userId,
                entity.getDepartmentId(),
                entity.getEmployeeCode(),
                entity.getFullName(),
                entity.getIsOutsourced(),
                entity.getStandardHoursPerWeek(),
                entity.getStatus()
        );
    }

    public EmployeeJpaEntity toJpaEntity(Employee domain) {
        if (domain == null) return null;
        return new EmployeeJpaEntity(
                domain.getIdValue(),
                domain.getUserIdValue(),
                domain.getDepartmentId(),
                domain.getEmployeeCode(),
                domain.getFullName(),
                domain.getIsOutsourced(),
                domain.getStandardHoursPerWeek(),
                domain.getStatus()
        );
    }

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) return null;
        return new AuditLog(
                entity.getId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getTableName(),
                entity.getRecordId(),
                entity.getCreatedAt()
        );
    }

    public AuditLogJpaEntity toJpaEntity(AuditLog domain) {
        if (domain == null) return null;
        return new AuditLogJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getAction(),
                domain.getTableName(),
                domain.getRecordId(),
                domain.getCreatedAt()
        );
    }
}
