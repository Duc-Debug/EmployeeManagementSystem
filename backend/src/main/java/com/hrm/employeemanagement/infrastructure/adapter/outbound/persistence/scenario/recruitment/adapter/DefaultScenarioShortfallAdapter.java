package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadScenarioShortfallPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;

@Component
public class DefaultScenarioShortfallAdapter implements LoadScenarioShortfallPort {

    private final LoadProjectRolePort loadProjectRolePort;
    private final LoadScenarioDemandPort loadScenarioDemandPort;

    public DefaultScenarioShortfallAdapter(
            LoadProjectRolePort loadProjectRolePort,
            @Autowired(required = false) LoadScenarioDemandPort loadScenarioDemandPort
    ) {
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
        this.loadScenarioDemandPort = loadScenarioDemandPort;
    }

    @Override
    public List<RoleShortfallDemand> loadShortfallDemands(Long scenarioId) {
        List<ProjectRole> activeRoles = loadProjectRolePort.findAllActive();
        if (activeRoles == null || activeRoles.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> shortfallByRoleId = new HashMap<>();

        if (scenarioId != null && loadScenarioDemandPort != null) {
            List<ScenarioDemand> demands = loadScenarioDemandPort.findByScenarioId(scenarioId);
            if (demands != null) {
                for (ScenarioDemand demand : demands) {
                    BigDecimal demandHours = calculateDemandTotalHours(demand);
                    if (demandHours.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    ProjectRole matchedRole = findMatchingRole(demand, activeRoles);
                    if (matchedRole != null && matchedRole.getId() != null) {
                        Long roleId = matchedRole.getId().value();
                        shortfallByRoleId.merge(roleId, demandHours, BigDecimal::add);
                    } else {
                        // Fallback vào vai trò đầu tiên nếu không khớp cụ thể
                        Long defaultRoleId = activeRoles.get(0).getId().value();
                        shortfallByRoleId.merge(defaultRoleId, demandHours, BigDecimal::add);
                    }
                }
            }
        }

        List<RoleShortfallDemand> result = new ArrayList<>();
        for (ProjectRole role : activeRoles) {
            Long roleId = role.getId() != null ? role.getId().value() : null;
            BigDecimal shortfall = roleId != null ? shortfallByRoleId.getOrDefault(roleId, BigDecimal.ZERO) : BigDecimal.ZERO;
            result.add(new RoleShortfallDemand(
                    roleId,
                    role.getCode(),
                    role.getName(),
                    shortfall
            ));
        }
        return result;
    }

    private BigDecimal calculateDemandTotalHours(ScenarioDemand demand) {
        if (demand == null || demand.getHeadcount() == null || demand.getHoursPerWeekPerPerson() == null) {
            return BigDecimal.ZERO;
        }
        int headcount = demand.getHeadcount();
        BigDecimal hoursPerWeek = demand.getHoursPerWeekPerPerson();
        int weeks = calculateWeeksCount(demand);

        return hoursPerWeek.multiply(BigDecimal.valueOf(headcount))
                .multiply(BigDecimal.valueOf(weeks));
    }

    private int calculateWeeksCount(ScenarioDemand demand) {
        if (demand.getStartYear() == null || demand.getStartWeek() == null
                || demand.getEndYear() == null || demand.getEndWeek() == null) {
            return 1;
        }
        try {
            YearWeek start = YearWeek.of(demand.getStartYear(), demand.getStartWeek());
            YearWeek end = YearWeek.of(demand.getEndYear(), demand.getEndWeek());
            if (start.isAfter(end)) {
                return 1;
            }
            int count = 0;
            YearWeek current = start;
            while (!current.isAfter(end)) {
                count++;
                int maxInYear = YearWeek.maxWeeksInYear(current.year());
                if (current.weekNumber() < maxInYear) {
                    current = YearWeek.of(current.year(), current.weekNumber() + 1);
                } else {
                    current = YearWeek.of(current.year() + 1, 1);
                }
                if (count > 200) break; // guard against infinite loop
            }
            return Math.max(1, count);
        } catch (Exception e) {
            return 1;
        }
    }

    private ProjectRole findMatchingRole(ScenarioDemand demand, List<ProjectRole> roles) {
        String demandName = demand.getDemandName() != null ? demand.getDemandName().trim().toLowerCase() : "";
        String skillReq = demand.getSkillRequirement() != null ? demand.getSkillRequirement().trim().toLowerCase() : "";

        // 1. Khớp chính xác code hoặc name
        for (ProjectRole role : roles) {
            String code = role.getCode() != null ? role.getCode().trim().toLowerCase() : "";
            String name = role.getName() != null ? role.getName().trim().toLowerCase() : "";
            if (!code.isEmpty() && (demandName.equalsIgnoreCase(code) || demandName.startsWith(code + " ") || demandName.contains("-" + code))) {
                return role;
            }
            if (!name.isEmpty() && demandName.equalsIgnoreCase(name)) {
                return role;
            }
        }

        // 2. Khớp chứa từ khóa
        for (ProjectRole role : roles) {
            String code = role.getCode() != null ? role.getCode().trim().toLowerCase() : "";
            String name = role.getName() != null ? role.getName().trim().toLowerCase() : "";
            if (!name.isEmpty() && (demandName.contains(name) || skillReq.contains(name))) {
                return role;
            }
            if (!code.isEmpty() && (demandName.contains(code) || skillReq.contains(code))) {
                return role;
            }
        }

        return null;
    }
}
