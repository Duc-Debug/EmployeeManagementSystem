package com.hrm.employeemanagement.domain.exception.role;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateProjectRoleCodeException extends DomainException {
    public DuplicateProjectRoleCodeException(String code) {
        super("Mã vai trò chuyên môn '" + code + "' đã tồn tại trong hệ thống.");
    }
}
