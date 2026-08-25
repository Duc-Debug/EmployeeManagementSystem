package com.hrm.employeemanagement.application.port.outbound.authorization;

import com.hrm.employeemanagement.domain.authorization.PermissionCode;

public interface PermissionQueryPort {

    boolean hasPermission(
            Long userId,
            PermissionCode permission
    );
}