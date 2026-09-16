package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadScenarioShortfallPort;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;

@Component
public class DefaultScenarioShortfallAdapter implements LoadScenarioShortfallPort {

    private final LoadProjectRolePort loadProjectRolePort;

    public DefaultScenarioShortfallAdapter(LoadProjectRolePort loadProjectRolePort) {
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
    }

    @Override
    public List<RoleShortfallDemand> loadShortfallDemands(Long scenarioId) {
        List<ProjectRole> activeRoles = loadProjectRolePort.findAllActive();
        List<RoleShortfallDemand> demands = new ArrayList<>();
        for (ProjectRole role : activeRoles) {
            // Trả về danh sách vai trò khả dụng trong hệ thống
            demands.add(new RoleShortfallDemand(
                    role.getId() != null ? role.getId().value() : null,
                    role.getCode(),
                    role.getName(),
                    BigDecimal.ZERO
            ));
        }
        return demands;
    }
}
