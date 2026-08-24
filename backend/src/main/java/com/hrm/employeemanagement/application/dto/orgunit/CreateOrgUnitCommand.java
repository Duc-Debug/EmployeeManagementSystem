package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

public record CreateOrgUnitCommand(
                String unitCode,
                String unitName,
                OrgUnitType unitType,
                Long parentId,
                Long managerId,
                String description) {
        public CreateOrgUnitCommand {
                if (managerId == null || managerId <= 0) {
                        throw new InvalidOrgUnitManagerException(
                                        "Người quản lý (managerId) không được để trống và phải lớn hơn 0");
                }
        }
}
