package com.hrm.employeemanagement.application.port.outbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;

public interface SaveScenarioSharePort {
    ScenarioShare save(ScenarioShare share);
    List<ScenarioShare> saveAll(List<ScenarioShare> shares);
}
