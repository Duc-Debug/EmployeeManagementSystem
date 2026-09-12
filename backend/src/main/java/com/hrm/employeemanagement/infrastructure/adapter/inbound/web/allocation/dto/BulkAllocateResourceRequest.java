package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * NCL-06-CN-006 & NCL-06-CN-007: Request DTO cho endpoint POST /api/v1/allocations/bulk, nhận payload phân bổ hàng loạt nhiều tuần từ client.
 */
public record BulkAllocateResourceRequest(
        @NotNull(message = "ID nhân sự không được null")
        Long employeeId,
        @NotNull(message = "ID dự án không được null")
        Long projectId,
        @NotNull(message = "Năm bắt đầu không được null")
        @Min(value = 2000, message = "Năm bắt đầu phải từ 2000 trở lên")
        @Max(value = 2100, message = "Năm bắt đầu không được vượt quá 2100")
        Integer fromYear,
        @NotNull(message = "Tuần bắt đầu không được null")
        @Min(value = 1, message = "Tuần bắt đầu phải từ 1 trở lên")
        @Max(value = 53, message = "Tuần bắt đầu tối đa là 53")
        Integer fromWeek,
        @NotNull(message = "Năm kết thúc không được null")
        @Min(value = 2000, message = "Năm kết thúc phải từ 2000 trở lên")
        @Max(value = 2100, message = "Năm kết thúc không được vượt quá 2100")
        Integer toYear,
        @NotNull(message = "Tuần kết thúc không được null")
        @Min(value = 1, message = "Tuần kết thúc phải từ 1 trở lên")
        @Max(value = 53, message = "Tuần kết thúc tối đa là 53")
        Integer toWeek,
        @DecimalMin(value = "0.1", message = "Số giờ phân bổ mỗi tuần phải lớn hơn 0")
        @DecimalMax(value = "168.0", message = "Số giờ phân bổ mỗi tuần không được vượt quá 168 giờ")
        BigDecimal allocatedHoursPerWeek,
        @DecimalMin(value = "0.0", message = "Tỷ lệ phần trăm phân bổ mỗi tuần không được là số âm")
        @DecimalMax(value = "100.0", message = "Tỷ lệ phần trăm phân bổ mỗi tuần tối đa là 100%")
        BigDecimal allocationPercentagePerWeek
) {
    public BulkAllocateResourceRequest(
            Long employeeId,
            Long projectId,
            Integer fromYear,
            Integer fromWeek,
            Integer toYear,
            Integer toWeek,
            BigDecimal allocatedHoursPerWeek
    ) {
        this(employeeId, projectId, fromYear, fromWeek, toYear, toWeek, allocatedHoursPerWeek, null);
    }

    public BulkAllocateResourceRequest {
        if (allocatedHoursPerWeek == null && allocationPercentagePerWeek == null) {
            throw new IllegalArgumentException("Phải cung cấp số giờ phân bổ hoặc tỷ lệ phần trăm phân bổ mỗi tuần");
        }
        if (allocatedHoursPerWeek != null && allocationPercentagePerWeek != null) {
            throw new IllegalArgumentException("Không được cung cấp đồng thời số giờ phân bổ và tỷ lệ phần trăm phân bổ mỗi tuần");
        }
    }
}
