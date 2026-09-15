package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ApplyRoleAllocationTemplateRequest(
        @NotEmpty(message = "Danh sách phân bổ vai trò không được để trống")
        @Valid
        List<RoleAssignmentRequest> assignments
) {
    public record RoleAssignmentRequest(
            @NotNull(message = "Vai trò không được để trống")
            Long roleId,

            Long employeeId,

            @NotNull(message = "Số giờ mỗi tuần không được để trống")
            @DecimalMin(value = "0.01", message = "Số giờ mỗi tuần phải lớn hơn 0")
            BigDecimal hoursPerWeek
    ) {}
}

