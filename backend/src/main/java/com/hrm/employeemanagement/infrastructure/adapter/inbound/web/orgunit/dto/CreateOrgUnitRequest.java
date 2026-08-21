package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrgUnitRequest(
    @NotBlank(message = "Unit code cannot be blank")
    @Size(max = 50, message = "Unit code cannot exceed 50 characters")
    String unitCode,

    @NotBlank(message = "Unit name cannot be blank")
    @Size(max = 255, message = "Unit name cannot exceed 255 characters")
    String unitName,

    @NotNull(message = "Unit type is required")
    OrgUnitType unitType,

    Long parentId,

    String description
) {
    public CreateOrgUnitCommand toCommand() {
        return new CreateOrgUnitCommand(
            this.unitCode,
            this.unitName,
            this.unitType,
            this.parentId,
            this.description
        );
    }
}