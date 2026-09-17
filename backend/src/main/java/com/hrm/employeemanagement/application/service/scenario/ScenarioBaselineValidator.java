package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.scenario.ScenarioAllocationSnapshotItem;

/**
 * Validator kiểm tra tính tươi mới của Baseline Snapshot cho Kịch bản nguồn lực.
 * Đảm bảo phát hiện stale cả về giờ phân bổ (allocatedHours) và giờ khả dụng (availableHours).
 */
@Component
public class ScenarioBaselineValidator {

    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public ScenarioBaselineValidator(
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
    }

    public List<String> checkBaselineStale(
            List<ScenarioAllocationSnapshotItem> snapshotItems,
            List<YearWeek> targetWeeks,
            Map<Long, Employee> employeeMap
    ) {
        if (snapshotItems == null || snapshotItems.isEmpty() || targetWeeks == null || targetWeeks.isEmpty()) {
            return List.of();
        }

        List<Long> snapshotEmpIds = snapshotItems.stream()
                .map(ScenarioAllocationSnapshotItem::getEmployeeId)
                .distinct()
                .toList();

        // 1. Nạp và gom phân bổ hiện tại
        List<WeeklyProjectAllocation> currentAllocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(snapshotEmpIds, targetWeeks);
        Map<String, BigDecimal> currentAllocMap = currentAllocations.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        WeeklyProjectAllocation::getAllocatedHours,
                        BigDecimal::add
                ));

        // 2. Tính khả dụng thực tế hiện tại
        Map<String, BigDecimal> currentAvailMap = calculateCurrentAvailableHours(snapshotEmpIds, targetWeeks, employeeMap);

        // 3. Kiểm tra stale cả allocatedHours và availableHours
        List<String> staleReasons = new ArrayList<>();
        for (ScenarioAllocationSnapshotItem item : snapshotItems) {
            String key = makeKey(item.getEmployeeId(), item.getYearNumber(), item.getWeekNumber());
            BigDecimal currentAllocated = currentAllocMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal currentAvailable = currentAvailMap.getOrDefault(key, BigDecimal.ZERO);

            Employee emp = employeeMap.get(item.getEmployeeId());
            String empName = emp != null ? emp.getFullName() : "ID " + item.getEmployeeId();

            if (currentAllocated.compareTo(item.getAllocatedHours()) != 0) {
                staleReasons.add(String.format(
                        "Nhân sự %s: phân bổ tuần %d/%d gốc đã đổi từ %s giờ sang %s giờ",
                        empName,
                        item.getWeekNumber(),
                        item.getYearNumber(),
                        item.getAllocatedHours().stripTrailingZeros().toPlainString(),
                        currentAllocated.stripTrailingZeros().toPlainString()
                ));
            }

            if (item.getAvailableHours() != null && currentAvailable.compareTo(item.getAvailableHours()) != 0) {
                staleReasons.add(String.format(
                        "Nhân sự %s: khả dụng tuần %d/%d gốc đã đổi từ %s giờ sang %s giờ",
                        empName,
                        item.getWeekNumber(),
                        item.getYearNumber(),
                        item.getAvailableHours().stripTrailingZeros().toPlainString(),
                        currentAvailable.stripTrailingZeros().toPlainString()
                ));
            }
        }
        return staleReasons;
    }

    public Map<String, BigDecimal> calculateCurrentAvailableHours(
            List<Long> empIds,
            List<YearWeek> targetWeeks,
            Map<Long, Employee> employeeMap
    ) {
        if (empIds.isEmpty() || targetWeeks.isEmpty()) {
            return Map.of();
        }

        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(empIds, targetWeeks);
        Map<String, WeeklyAvailability> availabilityMap = availabilities.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (e1, e2) -> e1
                ));

        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(empIds, targetWeeks);

        LocalDate minStart = targetWeeks.get(0).getStartDate();
        LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        int weekWorkingDaysCount = workingDays.isEmpty() ? 5 : workingDays.size();

        Map<String, BigDecimal> result = new HashMap<>();
        for (Long empId : empIds) {
            Employee emp = employeeMap.get(empId);
            for (YearWeek yw : targetWeeks) {
                String key = makeKey(empId, yw.year(), yw.weekNumber());

                WeeklyAvailability savedAvail = availabilityMap.get(key);
                BigDecimal baseAvailable;
                if (savedAvail != null) {
                    baseAvailable = savedAvail.getNetAvailableHours();
                } else {
                    int standardHours = (emp != null && emp.getStandardHoursPerWeek() != null) ? emp.getStandardHoursPerWeek() : 40;
                    int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                    BigDecimal leaveHours = leaveHoursMap.getOrDefault(empId, Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                    baseAvailable = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
                }

                BigDecimal availableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                        baseAvailable,
                        emp != null ? emp.getContractEndDate() : null,
                        yw.getStartDate(),
                        yw.getEndDate(),
                        weekWorkingDaysCount
                );
                result.put(key, availableHours);
            }
        }
        return result;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
    }
}
