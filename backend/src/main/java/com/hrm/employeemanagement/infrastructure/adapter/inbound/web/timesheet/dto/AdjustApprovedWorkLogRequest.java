package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustApprovedWorkLogRequest(
        @NotNull(message = "Số giờ làm việc không được để trống.")
        @DecimalMin(value = "0.01", message = "Số giờ làm việc phải lớn hơn 0.")
        @DecimalMax(value = "24.00", message = "Số giờ làm việc không được vượt quá 24 giờ.")
        BigDecimal hours,

        Long taskId,
        Boolean isBillable,
        String description,

        @NotBlank(message = "Lý do điều chỉnh không được để trống.")
        @Size(min = 10, message = "Lý do điều chỉnh phải có ít nhất 10 ký tự.")
        String reason,

        Long version
) {
}
