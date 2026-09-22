package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto;

import jakarta.validation.constraints.Size;

public record RejectUnavailabilityRequest(
        @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
        String rejectReason
) {}
