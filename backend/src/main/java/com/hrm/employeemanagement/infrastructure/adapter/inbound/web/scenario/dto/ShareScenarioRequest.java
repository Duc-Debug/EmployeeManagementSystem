package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record ShareScenarioRequest(
        @NotEmpty(message = "Danh sách người nhận chia sẻ không được để trống")
        List<Long> userIds
) {
}
