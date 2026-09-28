package com.hrm.employeemanagement.application.port.outbound.scenario;

import com.hrm.employeemanagement.domain.scenario.ResourceScenario;

public interface SaveResourceScenarioPort {
    ResourceScenario save(ResourceScenario scenario);
}
