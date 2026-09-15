package com.hrm.employeemanagement.application.port.outbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;

public interface LoadScenarioSnapshotPort {
    List<ScenarioAllocationSnapshotItem> findByScenarioId(Long scenarioId);
}
