package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReopenProjectRequest(
        @NotBlank(message = "Lý do mở lại dự án không được để trống")
        @Size(min = 10, max = 500, message = "Lý do mở lại dự án phải có từ 10 đến 500 ký tự")
        String reopenReason) {
}
