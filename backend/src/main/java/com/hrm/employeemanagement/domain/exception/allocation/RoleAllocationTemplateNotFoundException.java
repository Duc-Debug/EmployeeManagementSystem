package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class RoleAllocationTemplateNotFoundException extends DomainException {
    public RoleAllocationTemplateNotFoundException(Long templateId) {
        super("Không tìm thấy mẫu phân bổ vai trò với ID: " + templateId);
    }

    public RoleAllocationTemplateNotFoundException(String message) {
        super(message);
    }
}

