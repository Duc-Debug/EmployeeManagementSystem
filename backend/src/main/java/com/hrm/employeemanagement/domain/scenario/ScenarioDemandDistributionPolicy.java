package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;

/**
 * Domain policy chịu trách nhiệm khớp vai trò nhân sự và phân bổ số giờ nhu cầu (ScenarioDemand)
 * đều cho các nhân sự phù hợp theo tuần, bảo toàn phần dư làm tròn (remainder cents).
 */
public class ScenarioDemandDistributionPolicy {

    public static boolean isRoleMatching(String employeeRole, String requirement) {
        if (requirement == null || requirement.trim().isEmpty()) {
            return true;
        }
        if (employeeRole == null || employeeRole.trim().isEmpty()) {
            return false;
        }
        String trimmedReq = requirement.trim().toLowerCase();
        String trimmedRole = employeeRole.trim().toLowerCase();
        if (trimmedRole.equals(trimmedReq)) {
            return true;
        }
        String regex = "(?i)(^|[^a-zA-Z0-9_#+])" + Pattern.quote(trimmedReq) + "([^a-zA-Z0-9_#+]|$)";
        return Pattern.compile(regex).matcher(trimmedRole).find();
    }

    public static Map<Long, Map<String, BigDecimal>> calculateDistribution(
            List<ScenarioDemand> demands,
            List<Long> empIds,
            Map<Long, Employee> employeeMap,
            List<YearWeek> targetWeeks
    ) {
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = new HashMap<>();

        for (YearWeek yw : targetWeeks) {
            List<ScenarioDemand> activeDemands = demands.stream()
                    .filter(d -> d.isActiveInWeek(yw))
                    .toList();

            for (ScenarioDemand d : activeDemands) {
                BigDecimal totalDemandHours = d.getTotalHoursPerWeek();
                if (totalDemandHours.compareTo(BigDecimal.ZERO) <= 0) continue;

                String req = d.getSkillRequirement();
                List<Long> matchingEmpIds = empIds.stream()
                        .filter(id -> {
                            Employee emp = employeeMap.get(id);
                            return emp != null && isRoleMatching(emp.getProfessionalRole(), req);
                        })
                        .toList();

                if (!matchingEmpIds.isEmpty()) {
                    int count = matchingEmpIds.size();
                    BigDecimal basePerEmp = totalDemandHours.divide(BigDecimal.valueOf(count), 2, RoundingMode.FLOOR);
                    BigDecimal allocatedSoFar = basePerEmp.multiply(BigDecimal.valueOf(count));
                    int remainderCents = totalDemandHours.subtract(allocatedSoFar).movePointRight(2).intValue();

                    for (int i = 0; i < count; i++) {
                        Long empId = matchingEmpIds.get(i);
                        BigDecimal empHours = (i < remainderCents)
                                ? basePerEmp.add(new BigDecimal("0.01"))
                                : basePerEmp;
                        String mapKey = makeKey(empId, yw.year(), yw.weekNumber());
                        empDemandHoursMap
                                .computeIfAbsent(empId, k -> new HashMap<>())
                                .merge(mapKey, empHours, BigDecimal::add);
                    }
                }
            }
        }

        return empDemandHoursMap;
    }

    public static String makeKey(Long employeeId, Integer year, Integer week) {
        return employeeId + "_" + year + "_" + week;
    }
}
