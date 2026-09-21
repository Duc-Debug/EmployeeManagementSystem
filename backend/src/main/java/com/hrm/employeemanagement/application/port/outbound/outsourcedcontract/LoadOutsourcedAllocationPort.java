package com.hrm.employeemanagement.application.port.outbound.outsourcedcontract;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.hrm.employeemanagement.domain.availability.YearWeek;

public interface LoadOutsourcedAllocationPort {

    record OutsourcedAllocationRecord(
            Long allocationId,
            Long employeeId,
            Long projectId,
            YearWeek yearWeek,
            BigDecimal allocatedHours
    ) {}

    List<OutsourcedAllocationRecord> findAllocationsByEmployeeId(Long employeeId);
    List<OutsourcedAllocationRecord> findAllocationsByEmployeeIds(List<Long> employeeIds);
    Map<Long, String> findProjectNamesByIds(List<Long> projectIds);
}
