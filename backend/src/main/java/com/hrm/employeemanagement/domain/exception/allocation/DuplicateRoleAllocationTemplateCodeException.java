package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateRoleAllocationTemplateCodeException extends DomainException {
    public DuplicateRoleAllocationTemplateCodeException(String code) {
        super("Mã mẫu phân bổ đã tồn tại: " + code);
    }
}

