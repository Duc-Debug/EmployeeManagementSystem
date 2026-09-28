package com.hrm.employeemanagement.application.port.outbound.allocation.period;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;

public interface LoadAllocationPlanningPeriodPort {

    Optional<AllocationPlanningPeriod> findById(Long id);

    List<AllocationPlanningPeriod> findAll(Integer year, AllocationPeriodStatus status);

    List<AllocationPlanningPeriod> findLockedPeriodsCoveringWeek(int year, int weekNumber);

    boolean existsOverlapping(Integer year, int startWeek, int endWeek, Long excludeId);
}
