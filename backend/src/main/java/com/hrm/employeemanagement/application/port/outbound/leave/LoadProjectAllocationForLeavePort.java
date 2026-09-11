package com.hrm.employeemanagement.application.port.outbound.leave;

import java.math.BigDecimal;
import java.util.List;

public interface LoadProjectAllocationForLeavePort {
    List<ProjectAllocationInfo> findAllocations(Long employeeId, int year, List<Integer> weekNumbers);

    record ProjectAllocationInfo(
            Long projectId,
            String projectName,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours
    ) {}
}
