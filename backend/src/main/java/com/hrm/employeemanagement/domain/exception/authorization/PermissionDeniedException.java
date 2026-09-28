package com.hrm.employeemanagement.domain.exception.authorization;

import com.hrm.employeemanagement.domain.authorization.PermissionCode;

public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(PermissionCode permission) {
        super("Không có quyền thực hiện thao tác: " + permission.name());
    }
}
