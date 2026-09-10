package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import jakarta.validation.constraints.Size;

public record CloseProjectRequest(
        @Size(max = 500, message = "Lý do đóng dự án không được vượt quá 500 ký tự")
        String closureReason) {
}
