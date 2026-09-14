package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.idleness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcknowledgeProlongedIdleStaffRequest(
        @NotNull(message = "Mã nhân viên không được để trống")
        Long employeeId,

        @NotBlank(message = "Hành động xử lý không được để trống")
        String actionTaken,

        String notes
) {
}
