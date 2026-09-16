package com.hrm.employeemanagement.application.port.outbound.scenario.recruitment;

import java.util.List;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;

public interface LoadScenarioShortfallPort {
    List<RoleShortfallDemand> loadShortfallDemands(Long scenarioId);
}
