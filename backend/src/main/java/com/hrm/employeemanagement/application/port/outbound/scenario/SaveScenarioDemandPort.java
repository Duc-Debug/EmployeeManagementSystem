package com.hrm.employeemanagement.application.port.outbound.scenario;

import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;

public interface SaveScenarioDemandPort {
    ScenarioDemand save(ScenarioDemand demand);
}
