package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
public record UpdateOrgUnitCommand(
    Long id,
    String unitName,
    OrgUnitType unitType,
    Long managerId,
    String description
) {
    public UpdateOrgUnitCommand {
        if (managerId == null || managerId <= 0) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) không được để trống và phải lớn hơn 0");
        }
    }
}
