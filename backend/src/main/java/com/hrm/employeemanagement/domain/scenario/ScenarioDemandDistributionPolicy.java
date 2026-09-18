package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;

/**
 * Domain policy chịu trách nhiệm khớp vai trò nhân sự và phân bổ số giờ nhu cầu (ScenarioDemand)
 * đều cho các nhân sự phù hợp theo tuần, bảo toàn phần dư làm tròn (remainder cents)
 * và đảm bảo không vượt quá capacity của nhân sự trong tuần.
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
        return calculateDistribution(demands, empIds, employeeMap, targetWeeks, null);
    }

    public static Map<Long, Map<String, BigDecimal>> calculateDistribution(
            List<ScenarioDemand> demands,
            List<Long> empIds,
            Map<Long, Employee> employeeMap,
            List<YearWeek> targetWeeks,
            Map<String, BigDecimal> capacityMap
    ) {
        Map<Long, Map<String, BigDecimal>> empDemandHoursMap = new HashMap<>();

        for (YearWeek yw : targetWeeks) {
            List<ScenarioDemand> activeDemands = demands.stream()
                    .filter(d -> d.isActiveInWeek(yw))
                    .toList();

            Map<Long, BigDecimal> allocatedInWeekMap = new HashMap<>();

            for (ScenarioDemand d : activeDemands) {
                BigDecimal totalDemandHours = d.getTotalHoursPerWeek();
                if (totalDemandHours == null || totalDemandHours.compareTo(BigDecimal.ZERO) <= 0) continue;

                String req = d.getSkillRequirement();
                List<Long> matchingEmpIds = empIds.stream()
                        .filter(id -> {
                            Employee emp = employeeMap.get(id);
                            return emp != null
                                    && emp.getStatus() == EmployeeStatus.ACTIVE
                                    && isRoleMatching(emp.getProfessionalRole(), req);
                        })
                        .toList();

                if (!matchingEmpIds.isEmpty()) {
                    int count = matchingEmpIds.size();
                    BigDecimal basePerEmp = totalDemandHours.divide(BigDecimal.valueOf(count), 2, RoundingMode.FLOOR);
                    BigDecimal allocatedSoFar = basePerEmp.multiply(BigDecimal.valueOf(count));
                    int remainderCents = totalDemandHours.subtract(allocatedSoFar).movePointRight(2).intValue();

                    for (int i = 0; i < count; i++) {
                        Long empId = matchingEmpIds.get(i);
                        BigDecimal desiredHours = (i < remainderCents)
                                ? basePerEmp.add(new BigDecimal("0.01"))
                                : basePerEmp;

                        String mapKey = makeKey(empId, yw.year(), yw.weekNumber());
                        BigDecimal empAllocatedInWeek = allocatedInWeekMap.getOrDefault(empId, BigDecimal.ZERO);
                        BigDecimal capacity = capacityMap != null && capacityMap.containsKey(mapKey)
                                ? capacityMap.get(mapKey)
                                : (employeeMap.get(empId) != null && employeeMap.get(empId).getStandardHoursPerWeek() != null
                                    ? BigDecimal.valueOf(employeeMap.get(empId).getStandardHoursPerWeek())
                                    : BigDecimal.valueOf(40));

                        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) < 0) {
                            capacity = BigDecimal.ZERO;
                        }

                        BigDecimal remainingCap = capacity.subtract(empAllocatedInWeek).max(BigDecimal.ZERO);
                        BigDecimal empHours = desiredHours.min(remainingCap);

                        if (empHours.compareTo(BigDecimal.ZERO) > 0) {
                            allocatedInWeekMap.put(empId, empAllocatedInWeek.add(empHours));
                            empDemandHoursMap
                                    .computeIfAbsent(empId, k -> new HashMap<>())
                                    .merge(mapKey, empHours, BigDecimal::add);
                        }
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
