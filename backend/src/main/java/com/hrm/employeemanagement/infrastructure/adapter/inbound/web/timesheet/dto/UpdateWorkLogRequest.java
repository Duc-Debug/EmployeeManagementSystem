package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkLogRequest(
        @NotNull(message = "Dự án không được để trống.")
        Long projectId,

        @NotNull(message = "Công việc không được để trống.")
        Long taskId,

        @NotNull(message = "Ngày làm việc không được để trống.")
        LocalDate workDate,

        @NotNull(message = "Số giờ làm việc không được để trống.")
        @DecimalMin(value = "0.01", message = "Số giờ làm việc phải lớn hơn 0.")
        @DecimalMax(value = "24.00", message = "Số giờ làm việc không được vượt quá 24 giờ.")
        BigDecimal hours,

        Boolean isBillable,

        @NotBlank(message = "Mô tả nội dung công việc không được để trống.")
        String description
) {
}
