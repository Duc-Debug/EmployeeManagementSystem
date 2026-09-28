package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.ProjectWeeklyAllocationResult;

public interface GetProjectWeeklyAllocationsUseCase {
    List<ProjectWeeklyAllocationResult> getByProject(Long projectId, Integer year, Integer startWeek, Integer endWeek);
}
