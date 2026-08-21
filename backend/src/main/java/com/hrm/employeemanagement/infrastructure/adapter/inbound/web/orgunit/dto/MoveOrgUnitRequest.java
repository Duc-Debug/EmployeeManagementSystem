package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.MoveOrgUnitCommand;
import jakarta.validation.constraints.NotNull;

public record MoveOrgUnitRequest(
        @NotNull(message = "New parent ID is required") Long newParentId) {
    public MoveOrgUnitCommand toCommand(Long id) {
        return new MoveOrgUnitCommand(id, this.newParentId);
    }
}