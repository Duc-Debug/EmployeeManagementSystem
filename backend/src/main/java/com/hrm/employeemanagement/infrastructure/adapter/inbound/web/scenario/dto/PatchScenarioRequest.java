package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import jakarta.validation.constraints.Size;

public record PatchScenarioRequest(
        @Size(max = 255, message = "Tên kịch bản không được vượt quá 255 ký tự")
        String name,

        @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
        String note
) {
}
