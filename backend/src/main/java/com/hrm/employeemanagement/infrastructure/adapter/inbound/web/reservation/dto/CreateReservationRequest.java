package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateReservationRequest(
        @NotNull(message = "Mã dự án (projectId) không được để trống")
        Long projectId,

        @NotNull(message = "Mã nhân sự (employeeId) không được để trống")
        Long employeeId,

        @NotNull(message = "Năm không được để trống")
        @Min(value = 2000, message = "Năm phải từ 2000 trở lên")
        @Max(value = 2100, message = "Năm không được vượt quá 2100")
        Integer year,

        @NotNull(message = "Tuần không được để trống")
        @Min(value = 1, message = "Số tuần phải từ 1 đến 53")
        @Max(value = 53, message = "Số tuần không được vượt quá 53")
        Integer weekNumber,

        @NotNull(message = "Số giờ giữ chỗ không được để trống")
        @DecimalMin(value = "0.01", message = "Số giờ giữ chỗ phải lớn hơn 0")
        @DecimalMax(value = "168.00", message = "Số giờ giữ chỗ không được vượt quá 168 giờ")
        BigDecimal reservedHours,

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note
) {}
