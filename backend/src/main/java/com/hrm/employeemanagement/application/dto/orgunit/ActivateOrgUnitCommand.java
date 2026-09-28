package com.hrm.employeemanagement.application.dto.orgunit;

import java.util.Objects;

public record ActivateOrgUnitCommand(Long id) {
    public ActivateOrgUnitCommand {
        Objects.requireNonNull(id, "ID đơn vị tổ chức không được để trống");
    }
}
