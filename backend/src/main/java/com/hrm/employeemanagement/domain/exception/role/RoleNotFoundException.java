package com.hrm.employeemanagement.domain.exception.role;

import com.hrm.employeemanagement.domain.exception.DomainException;
import com.hrm.employeemanagement.domain.role.RoleCode;

public class RoleNotFoundException extends DomainException {
    public RoleNotFoundException(RoleCode roleCode) {
        super("Không tìm thấy vai trò với mã: " + (roleCode != null ? roleCode.getCode() : "null"));
    }

    public RoleNotFoundException(String message) {
        super(message);
    }
}