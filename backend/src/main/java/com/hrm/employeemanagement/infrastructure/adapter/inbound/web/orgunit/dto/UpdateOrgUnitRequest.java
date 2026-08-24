package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.UpdateOrgUnitCommand;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrgUnitRequest(
        @NotBlank(message = "Unit name cannot be blank") @Size(max = 255, message = "Unit name cannot exceed 255 characters") String unitName,
        @NotNull(message = "Unit type is required") OrgUnitType unitType,
        Long managerId,
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