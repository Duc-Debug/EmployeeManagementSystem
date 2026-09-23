package com.hrm.employeemanagement.application.port.outbound.report.billablerate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LoadBillableRateTimesheetPort {

    Map<Long, EmployeeHoursData> loadApprovedHoursByEmployeesAndDateRange(
            List<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate
    );

    record EmployeeHoursData(BigDecimal billableHours, BigDecimal nonBillableHours) {
        public EmployeeHoursData {
            if (billableHours == null) billableHours = BigDecimal.ZERO;
            if (nonBillableHours == null) nonBillableHours = BigDecimal.ZERO;
        }

        public BigDecimal totalActualHours() {
            return billableHours.add(nonBillableHours);
        }
    }
}
