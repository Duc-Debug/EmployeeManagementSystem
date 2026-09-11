package com.hrm.employeemanagement.domain.exception.role;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateProjectRoleNameException extends DomainException {
    public DuplicateProjectRoleNameException(String name) {
        super("Tên vai trò chuyên môn '" + name + "' đã tồn tại trong hệ thống.");
    }
}
