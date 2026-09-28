package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report.timesheetvariance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.report.timesheetvariance.LoadTimesheetVariancePort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetEntryRepository;

@Component
public class JpaTimesheetVarianceRepositoryAdapter implements LoadTimesheetVariancePort {

    private final SpringDataTimesheetEntryRepository timesheetEntryRepository;

    public JpaTimesheetVarianceRepositoryAdapter(SpringDataTimesheetEntryRepository timesheetEntryRepository) {
        this.timesheetEntryRepository = timesheetEntryRepository;
    }

    @Override
    public Map<String, BigDecimal> loadApprovedActualHours(List<Long> employeeIds, List<YearWeek> targetWeeks, Long projectId) {
        if (employeeIds == null || employeeIds.isEmpty() || targetWeeks == null || targetWeeks.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDate minStart = targetWeeks.stream().map(YearWeek::getStartDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate maxEnd = targetWeeks.stream().map(YearWeek::getEndDate).max(LocalDate::compareTo).orElseThrow();

        List<Object[]> results = timesheetEntryRepository.sumApprovedHoursByEmployeesAndDateRange(
                employeeIds, minStart, maxEnd, projectId
        );

        Map<String, BigDecimal> actualHoursMap = new HashMap<>();

        for (Object[] row : results) {
            if (row == null || row.length < 4 || row[0] == null || row[1] == null) {
                continue;
            }

            Long empId = ((Number) row[0]).longValue();
            Long projId = ((Number) row[1]).longValue();

            LocalDate workDate = null;
            if (row[2] instanceof LocalDate ld) {
                workDate = ld;
            } else if (row[2] instanceof java.sql.Date sd) {
                workDate = sd.toLocalDate();
            } else if (row[2] instanceof java.util.Date ud) {
                workDate = new java.sql.Date(ud.getTime()).toLocalDate();
            } else if (row[2] != null) {
                try {
                    workDate = LocalDate.parse(row[2].toString());
                } catch (Exception ignored) {
                }
            }

            BigDecimal hours = BigDecimal.ZERO;
            if (row[3] instanceof BigDecimal bd) {
                hours = bd;
            } else if (row[3] instanceof Number num) {
                hours = BigDecimal.valueOf(num.doubleValue());
            } else if (row[3] != null) {
                try {
                    hours = new BigDecimal(row[3].toString());
                } catch (Exception ignored) {
                }
            }

            if (workDate != null && hours != null) {
                YearWeek yw = YearWeek.from(workDate);
                // Key theo employee, project và tuần: employeeId_projectId_year_week
                String detailedKey = empId + "_" + projId + "_" + yw.year() + "_" + yw.weekNumber();
                actualHoursMap.merge(detailedKey, hours, BigDecimal::add);

                // Key theo employee và tuần: employeeId_year_week
                String empWeekKey = empId + "_" + yw.year() + "_" + yw.weekNumber();
                actualHoursMap.merge(empWeekKey, hours, BigDecimal::add);
            }
        }

        return actualHoursMap;
    }

    @Override
    public Map<String, Boolean> checkApprovedTimesheetExistence(List<Long> employeeIds, List<YearWeek> targetWeeks) {
        if (employeeIds == null || employeeIds.isEmpty() || targetWeeks == null || targetWeeks.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> hoursMap = loadApprovedActualHours(employeeIds, targetWeeks, null);
        Map<String, Boolean> existenceMap = new HashMap<>();

        for (Long empId : employeeIds) {
            for (YearWeek yw : targetWeeks) {
                String key = empId + "_" + yw.year() + "_" + yw.weekNumber();
                BigDecimal hours = hoursMap.get(key);
                existenceMap.put(key, hours != null && hours.compareTo(BigDecimal.ZERO) > 0);
            }
        }

        return existenceMap;
    }
}
