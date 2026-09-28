package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoleAllocationTemplateRequest(
        @NotBlank(message = "Mã mẫu phân bổ không được để trống")
        @Size(max = 50, message = "Mã mẫu không vượt quá 50 ký tự")
        String templateCode,

        @NotBlank(message = "Tên mẫu phân bổ không được để trống")
        @Size(max = 255, message = "Tên mẫu không vượt quá 255 ký tự")
        String name,

        String description,

        Long sourceProjectId,

        @NotEmpty(message = "Mẫu phân bổ phải có ít nhất 1 vai trò")
        @Valid
        List<TemplateItemRequest> items
) {
    public record TemplateItemRequest(
            @NotNull(message = "Vai trò không được để trống")
            Long roleId,

            @NotNull(message = "Số giờ mỗi tuần không được để trống")
            @DecimalMin(value = "0.01", message = "Số giờ mỗi tuần phải lớn hơn 0")
            @DecimalMax(value = "168.00", message = "Số giờ mỗi tuần không được vượt quá 168")
            BigDecimal hoursPerWeek
    ) {}
}

