package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlockPeriodRequest(
        @NotBlank(message = "Lý do mở lại kỳ kế hoạch không được để trống")
        @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
        String reason
) {
}
