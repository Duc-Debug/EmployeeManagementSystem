package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;

public interface LoadWeeklyProjectAllocationPort {

    Optional<WeeklyProjectAllocation> loadAllocation(Long employeeId, Long projectId, YearWeek yearWeek);

    List<WeeklyProjectAllocation> loadAllocationsForEmployee(Long employeeId, YearWeek yearWeek);

    List<WeeklyProjectAllocation> loadAllocationsForEmployeesInWeekRange(List<Long> employeeIds, Integer year, Integer startWeek, Integer endWeek);

    List<WeeklyProjectAllocation> loadAllocationsForEmployeesAndWeeks(List<Long> employeeIds, List<YearWeek> targetWeeks);
}
