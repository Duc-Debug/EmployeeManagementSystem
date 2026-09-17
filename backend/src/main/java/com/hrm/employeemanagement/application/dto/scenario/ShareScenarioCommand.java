package com.hrm.employeemanagement.application.dto.scenario;

import java.util.List;

public record ShareScenarioCommand(
        Long scenarioId,
        List<Long> userIds
) {
}
