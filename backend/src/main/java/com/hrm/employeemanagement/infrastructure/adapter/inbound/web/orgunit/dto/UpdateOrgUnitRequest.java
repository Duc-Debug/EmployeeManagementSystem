package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.UpdateOrgUnitCommand;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateOrgUnitRequest(
        @NotBlank(message = "Tên đơn vị không được để trống.") @Size(max = 255, message = "Tên đơn vị không được vượt quá 255 ký tự.") String unitName,
        @NotNull(message = "Loại đơn vị không được null") OrgUnitType unitType,
        @NotNull(message = "Người quản lý không được để trống") @Positive(message = "ID người quản lý phải hợp lệ") Long managerId,
        String description) {
    public UpdateOrgUnitCommand toCommand(Long id) {
        return new UpdateOrgUnitCommand(
                id,
                this.unitName,
                this.unitType,
                this.managerId,
                this.description);
    }
}