package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateOrgUnitRequest(
    @NotBlank(message = "Unit code cannot be blank")
    @Size(max = 50, message = "Unit code cannot exceed 50 characters")
    String unitCode,

    @NotBlank(message = "Unit name cannot be blank")
    @Size(max = 255, message = "Unit name cannot exceed 255 characters")
    String unitName,

    @NotNull(message = "Unit type is required")
    OrgUnitType unitType,

    @Positive(message = "Parent ID must be positive and greater than 0")
    Long parentId,

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description
) {
    public CreateOrgUnitCommand toCommand() {
        String normalizedCode = this.unitCode != null ? this.unitCode.trim().toUpperCase(Locale.ROOT) : null;
        String trimmedName = this.unitName != null ? this.unitName.trim() : null;
        String trimmedDescription = this.description != null ? this.description.trim() : null;

        return new CreateOrgUnitCommand(
            normalizedCode,
            trimmedName,
            this.unitType,
            this.parentId,
            trimmedDescription
        );
    }
}