package com.hrm.employeemanagement.application.port.inbound.milestone;

import java.util.List;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;

public interface GetProjectMilestonesUseCase {
    List<MilestoneResult> getProjectMilestones(Long projectId);
}
