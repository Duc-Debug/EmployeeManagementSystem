package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;

public record MoveOrgUnitCommand(
        Long id,
        Long newParentId
) {
    public MoveOrgUnitCommand {
        if (id == null) {
            throw RequiredFieldMissingException.of("ID đơn vị cần di chuyển");
        }
        if (newParentId == null) {
            throw RequiredFieldMissingException.of("ID đơn vị cha mới");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("ID đơn vị cần di chuyển phải lớn hơn 0");
        }
        if (newParentId <= 0) {
            throw new IllegalArgumentException("ID đơn vị cha mới phải lớn hơn 0");
        }
    }
}
