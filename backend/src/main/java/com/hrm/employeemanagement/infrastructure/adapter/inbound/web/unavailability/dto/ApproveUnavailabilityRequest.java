package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto;

import jakarta.validation.constraints.Size;

public record ApproveUnavailabilityRequest(
        @Size(max = 500, message = "Ý kiến người phê duyệt không được vượt quá 500 ký tự")
        String approverComment,
        Boolean confirmConflictWarning
) {}
