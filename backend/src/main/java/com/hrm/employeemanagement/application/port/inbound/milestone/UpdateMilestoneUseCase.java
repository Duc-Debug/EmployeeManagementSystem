package com.hrm.employeemanagement.application.port.inbound.milestone;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.dto.milestone.UpdateMilestoneCommand;

public interface UpdateMilestoneUseCase {
    MilestoneResult updateMilestone(UpdateMilestoneCommand command);
}
