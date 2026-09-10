package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.authorization;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;

@Component
public class PermissionQueryAdapter
        implements PermissionQueryPort {

    private final SpringDataPermissionRepository repository;

    public PermissionQueryAdapter(
            SpringDataPermissionRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "SpringDataPermissionRepository must not be null"
        );
    }

    @Override
    public boolean hasPermission(
            Long userId,
            PermissionCode permission
    ) {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        Objects.requireNonNull(
                permission,
                "permission must not be null"
        );

        if (repository.countPermissionMatches(userId, permission.name()) > 0) {
            return true;
        }

        // Domain-level RBAC safety guarantee for Working Calendar:
        // VT-05 (HR) and VT-06 (Admin) always have WORKING_CALENDAR_MANAGE & WORKING_CALENDAR_READ
        // VT-01, VT-02, VT-03, VT-04 have WORKING_CALENDAR_READ only
        if (permission == PermissionCode.WORKING_CALENDAR_MANAGE) {
            return repository.countUserRoleMatches(userId, "VT-05") > 0
                    || repository.countUserRoleMatches(userId, "VT-06") > 0;
        }
        if (permission == PermissionCode.WORKING_CALENDAR_READ) {
            return repository.countUserRoleMatches(userId, "VT-01") > 0
                    || repository.countUserRoleMatches(userId, "VT-02") > 0
                    || repository.countUserRoleMatches(userId, "VT-03") > 0
                    || repository.countUserRoleMatches(userId, "VT-04") > 0
                    || repository.countUserRoleMatches(userId, "VT-05") > 0
                    || repository.countUserRoleMatches(userId, "VT-06") > 0;
        }

        return false;
    }
}