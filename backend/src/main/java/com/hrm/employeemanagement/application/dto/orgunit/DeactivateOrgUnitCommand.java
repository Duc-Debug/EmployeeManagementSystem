package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;

public record DeactivateOrgUnitCommand(
        Long id
) {
    public DeactivateOrgUnitCommand {
        if (id == null) {
            throw RequiredFieldMissingException.of("ID đơn vị tổ chức");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("ID đơn vị tổ chức phải lớn hơn 0");
        }
    }
}
