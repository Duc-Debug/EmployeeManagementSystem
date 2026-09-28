package com.hrm.employeemanagement.application.dto.scenario;

import java.util.List;

public record ScenarioDetailResult(
        ScenarioResult scenario,
        List<ScenarioDemandResult> demands
) {
}
