package com.hrm.employeemanagement.application.port.inbound.milestone;

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;
import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;

public interface CreateMilestoneUseCase {
    MilestoneResult createMilestone(CreateMilestoneCommand command);
}
