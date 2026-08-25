package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateOrgUnitRequest(
        @NotBlank(message = "Mã đơn vị không được để trống") @Size(max = 50, message = "Mã đơn vị không được vượt quá 50 ký tự") 
        String unitCode,

        @NotBlank(message = "Tên đơn vị không được để trống") @Size(max = 255, message = "Tên đơn vị không được vượt quá 255 ký tự") String unitName,
        @NotNull(message = "Loại đơn vị là bắt buộc") 
        OrgUnitType unitType,

        @Positive(message = "Parent ID phải lớn hơn 0") 
        Long parentId,
        @NotNull(message = "Người quản lý không được để trống") @Positive(message = "ID người quản lý phải hợp lệ") 
        Long managerId,

        @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự") 
        String description) {
    public CreateOrgUnitCommand toCommand() {
        String normalizedCode = this.unitCode != null ? this.unitCode.trim().toUpperCase(Locale.ROOT) : null;
        String trimmedName = this.unitName != null ? this.unitName.trim() : null;
        String trimmedDescription = this.description != null ? this.description.trim() : null;

        return new CreateOrgUnitCommand(
                normalizedCode,
                trimmedName,
                this.unitType,
                this.parentId,
                this.managerId,
                trimmedDescription);
    }
}