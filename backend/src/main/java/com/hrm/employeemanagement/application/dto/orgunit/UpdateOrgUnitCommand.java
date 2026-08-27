package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

public record UpdateOrgUnitCommand(
        Long id,
        String unitName,
        OrgUnitType unitType,
        Long managerId,
        String description
) {
    public UpdateOrgUnitCommand {
        if (id == null) {
            throw RequiredFieldMissingException.of("ID đơn vị tổ chức");
        }
        if (unitName == null || unitName.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên đơn vị (unitName)");
        }
        if (unitType == null) {
            throw RequiredFieldMissingException.of("Loại đơn vị (unitType)");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("ID đơn vị tổ chức phải lớn hơn 0");
        }
        if (managerId != null && managerId <= 0) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) phải lớn hơn 0");
        }
        if (unitName.trim().length() > 255) {
            throw new IllegalArgumentException("Tên đơn vị không được vượt quá 255 ký tự");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 2000 ký tự");
        }

        unitName = unitName.trim();
        description = description != null ? description.trim() : null;
    }
}
