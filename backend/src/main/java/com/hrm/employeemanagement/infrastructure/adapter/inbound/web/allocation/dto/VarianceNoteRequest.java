package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto;

import jakarta.validation.constraints.NotBlank;

public record VarianceNoteRequest(
        @NotBlank(message = "Lý do chênh lệch không được để trống")
        String varianceReason
) {
}
