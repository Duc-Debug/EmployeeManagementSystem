package com.hrm.employeemanagement.application.dto.scenario;

import java.time.LocalDateTime;
import java.util.List;

public record ScenarioSnapshotData(
        int snapshotVersion,
        LocalDateTime savedAt,
        Long savedBy,
        String savedByName,
        List<Long> projectIds,
        List<String> projectNames,
        ScenarioResult scenario,
        List<ScenarioDemandResult> demands,
        ScenarioSimulationResult simulationResult
) {
}
