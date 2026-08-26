package com.hrm.employeemanagement.application.dto.orgunit;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

public record CreateOrgUnitCommand(
        String unitCode,
        String unitName,
        OrgUnitType unitType,
        Long parentId,
        Long managerId,
        String description
) {
    public CreateOrgUnitCommand {
        if (unitCode == null || unitCode.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Mã đơn vị (unitCode)");
        }
        if (unitCode.trim().length() > 50) {
            throw new IllegalArgumentException("Mã đơn vị không được vượt quá 50 ký tự");
        }
        if (unitName == null || unitName.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên đơn vị (unitName)");
        }
        if (unitName.trim().length() > 255) {
            throw new IllegalArgumentException("Tên đơn vị không được vượt quá 255 ký tự");
        }
        if (unitType == null) {
            throw RequiredFieldMissingException.of("Loại đơn vị (unitType)");
        }
        if (parentId != null && parentId <= 0) {
            throw new IllegalArgumentException("ID đơn vị cha phải lớn hơn 0");
        }
        if (managerId != null && managerId <= 0) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) phải lớn hơn 0");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 2000 ký tự");
        }

        // Tự động chuẩn hóa dữ liệu đầu vào
        unitCode = unitCode.trim().toUpperCase();
        unitName = unitName.trim();
        description = description != null ? description.trim() : null;
    }
}
