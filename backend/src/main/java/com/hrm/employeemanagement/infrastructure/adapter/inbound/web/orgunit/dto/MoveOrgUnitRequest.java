package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto;

import com.hrm.employeemanagement.application.dto.orgunit.MoveOrgUnitCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoveOrgUnitRequest(
        @NotNull(message = "Cần ID của nút cha") @Positive(message = "Id nút cha mới phải là số dương và lớn hơn 0.") Long newParentId) {
    public MoveOrgUnitCommand toCommand(Long id) {
        return new MoveOrgUnitCommand(id, this.newParentId);
    }
}