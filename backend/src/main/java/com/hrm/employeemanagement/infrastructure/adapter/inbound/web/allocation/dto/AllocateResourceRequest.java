package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AllocateResourceRequest(
        @NotNull(message = "ID nhân sự không được null")
        Long employeeId,
        @NotNull(message = "ID dự án không được null")
        Long projectId,
        @NotNull(message = "Năm không được null")
        @Min(value = 2000, message = "Năm phải từ 2000 trở lên")
        @Max(value = 2100, message = "Năm không được vượt quá 2100")
        Integer year,
        @NotNull(message = "Số tuần không được null")
        @Min(value = 1, message = "Số tuần phải từ 1 trở lên")
        @Max(value = 53, message = "Số tuần trong năm tối đa là 53")
        Integer weekNumber,
        @DecimalMin(value = "0.0", message = "Số giờ phân bổ không được là số âm")
        BigDecimal allocatedHours,
        @DecimalMin(value = "0.0", message = "Tỷ lệ phần trăm phân bổ không được là số âm")
        @DecimalMax(value = "100.0", message = "Tỷ lệ phần trăm phân bổ tối đa là 100%")
        BigDecimal allocationPercentage,
        String overloadReason
) {

    public AllocateResourceRequest(
            Long employeeId,
            Long projectId,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours
    ) {
        this(employeeId, projectId, year, weekNumber, allocatedHours, null, null);
    }

    public AllocateResourceRequest {
        if (allocatedHours == null && allocationPercentage == null) {
            throw new IllegalArgumentException("Phải cung cấp số giờ phân bổ hoặc tỷ lệ phần trăm phân bổ");
        }
    }
}
