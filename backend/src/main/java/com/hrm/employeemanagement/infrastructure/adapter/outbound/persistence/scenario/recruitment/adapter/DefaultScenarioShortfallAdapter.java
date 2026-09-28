package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DefaultScenarioShortfallAdapter.class);

    private final LoadProjectRolePort loadProjectRolePort;
    private final LoadScenarioDemandPort loadScenarioDemandPort;

    public DefaultScenarioShortfallAdapter(
            LoadProjectRolePort loadProjectRolePort,
            LoadScenarioDemandPort loadScenarioDemandPort
    ) {
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
        this.loadScenarioDemandPort = Objects.requireNonNull(loadScenarioDemandPort, "LoadScenarioDemandPort must not be null");
    }

    @Override
    public List<RoleShortfallDemand> loadShortfallDemands(Long scenarioId) {
        List<ProjectRole> activeRoles = loadProjectRolePort.findAllActive();
        if (activeRoles == null || activeRoles.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> shortfallByRoleId = new HashMap<>();

        if (scenarioId != null) {
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
                        log.warn("Bỏ qua nhu cầu kịch bản ID={} (demandName='{}', skillRequirement='{}') do không khớp với bất kỳ vai trò dự án nào trong hệ thống.",
                                demand.getId(), demand.getDemandName(), demand.getSkillRequirement());
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
        if (demand == null || roles == null || roles.isEmpty()) {
            return null;
        }

        String demandName = demand.getDemandName() != null ? demand.getDemandName().trim() : "";
        String skillReq = demand.getSkillRequirement() != null ? demand.getSkillRequirement().trim() : "";

        // Sắp xếp danh sách vai trò theo độ dài tên/mã giảm dần (longest match first)
        // để vai trò đặc hiệu nhất (ví dụ "Senior Developer") luôn được ưu tiên kiểm tra trước vai trò chung ("Developer")
        List<ProjectRole> sortedRoles = new ArrayList<>(roles);
        sortedRoles.sort((r1, r2) -> {
            int len1 = Math.max(r1.getName() != null ? r1.getName().length() : 0, r1.getCode() != null ? r1.getCode().length() : 0);
            int len2 = Math.max(r2.getName() != null ? r2.getName().length() : 0, r2.getCode() != null ? r2.getCode().length() : 0);
            return Integer.compare(len2, len1);
        });

        // 1. Tầng 1: Khớp chính xác 100% demandName với code hoặc name
        if (!demandName.isEmpty()) {
            for (ProjectRole role : sortedRoles) {
                if (role.getCode() != null && demandName.equalsIgnoreCase(role.getCode().trim())) {
                    return role;
                }
                if (role.getName() != null && demandName.equalsIgnoreCase(role.getName().trim())) {
                    return role;
                }
            }
        }

        // 2. Tầng 2: Khớp demandName theo ranh giới từ hoàn chỉnh (word boundary)
        if (!demandName.isEmpty()) {
            for (ProjectRole role : sortedRoles) {
                if (role.getCode() != null && matchesWordBoundary(demandName, role.getCode().trim())) {
                    return role;
                }
                if (role.getName() != null && matchesWordBoundary(demandName, role.getName().trim())) {
                    return role;
                }
            }
        }

        // 3. Tầng 3: Chỉ khi demandName không khớp bất kỳ vai trò nào mới xét đến skillRequirement
        if (!skillReq.isEmpty()) {
            for (ProjectRole role : sortedRoles) {
                if (role.getCode() != null && matchesWordBoundary(skillReq, role.getCode().trim())) {
                    return role;
                }
                if (role.getName() != null && matchesWordBoundary(skillReq, role.getName().trim())) {
                    return role;
                }
            }
        }

        return null;
    }

    private boolean matchesWordBoundary(String text, String target) {
        if (text == null || target == null || target.trim().isEmpty()) {
            return false;
        }
        String regex = "(?i)\\b" + Pattern.quote(target.trim()) + "\\b";
        return Pattern.compile(regex).matcher(text).find();
    }
}
