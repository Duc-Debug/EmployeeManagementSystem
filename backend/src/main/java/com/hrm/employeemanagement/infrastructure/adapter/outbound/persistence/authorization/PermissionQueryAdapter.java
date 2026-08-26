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

        return repository.countPermissionMatches(
                userId,
                permission.name()
        ) > 0;
    }
}