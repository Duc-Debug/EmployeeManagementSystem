package com.hrm.employeemanagement.application.port.outbound.authorization;

import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import java.util.List;

public interface PermissionQueryPort {

    boolean hasPermission(
            Long userId,
            PermissionCode permission
    );

    List<String> findPermissionsByUserId(Long userId);
}