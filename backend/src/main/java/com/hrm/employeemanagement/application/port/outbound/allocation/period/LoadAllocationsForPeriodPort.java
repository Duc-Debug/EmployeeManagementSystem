package com.hrm.employeemanagement.application.port.outbound.allocation.period;

import java.util.List;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;

/**
 * Cổng truy vấn các phân bổ trong dải tuần của kỳ phục vụ việc chụp bản chụp kế hoạch (Snapshot) theo TC-01.
 */
public interface LoadAllocationsForPeriodPort {

    List<WeeklyProjectAllocation> loadAllocationsInWeekRange(int year, int startWeek, int endWeek);
}
