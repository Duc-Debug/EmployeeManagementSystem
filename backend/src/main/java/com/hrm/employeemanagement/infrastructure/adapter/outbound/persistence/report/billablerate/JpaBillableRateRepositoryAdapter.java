package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report.billablerate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetEntryRepository;

@Component
public class JpaBillableRateRepositoryAdapter implements LoadBillableRateTimesheetPort {

    private final SpringDataTimesheetEntryRepository timesheetEntryRepository;

    public JpaBillableRateRepositoryAdapter(SpringDataTimesheetEntryRepository timesheetEntryRepository) {
        this.timesheetEntryRepository = timesheetEntryRepository;
    }

    @Override
    public Map<Long, EmployeeHoursData> loadApprovedHoursByEmployeesAndDateRange(
            List<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (employeeIds == null || employeeIds.isEmpty() || startDate == null || endDate == null) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = timesheetEntryRepository.sumApprovedHoursByEmployeesAndDateRangeGroupedByBillable(
                employeeIds, startDate, endDate
        );

        Map<Long, BigDecimal> billableMap = new HashMap<>();
        Map<Long, BigDecimal> nonBillableMap = new HashMap<>();

        for (Object[] row : rows) {
            if (row == null || row.length < 3 || row[0] == null) {
                continue;
            }

            Long empId = ((Number) row[0]).longValue();
            Boolean isBillable = row[1] != null && Boolean.parseBoolean(row[1].toString());

            BigDecimal hours = BigDecimal.ZERO;
            if (row[2] instanceof BigDecimal bd) {
                hours = bd;
            } else if (row[2] instanceof Number num) {
                hours = BigDecimal.valueOf(num.doubleValue());
            } else if (row[2] != null) {
                try {
                    hours = new BigDecimal(row[2].toString());
                } catch (Exception ignored) {
                }
            }

            if (Boolean.TRUE.equals(isBillable)) {
                billableMap.merge(empId, hours, BigDecimal::add);
            } else {
                nonBillableMap.merge(empId, hours, BigDecimal::add);
            }
        }

        Map<Long, EmployeeHoursData> resultMap = new HashMap<>();
        for (Long empId : employeeIds) {
            BigDecimal b = billableMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal nb = nonBillableMap.getOrDefault(empId, BigDecimal.ZERO);
            resultMap.put(empId, new EmployeeHoursData(b, nb));
        }

        return resultMap;
    }
}
