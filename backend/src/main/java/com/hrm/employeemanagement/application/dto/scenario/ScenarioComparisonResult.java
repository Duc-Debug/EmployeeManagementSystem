package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;
import java.util.List;

public record ScenarioComparisonResult(
        List<ScenarioComparisonItemResult> scenarios,
        LocalDateTime comparedAt
) {
}
