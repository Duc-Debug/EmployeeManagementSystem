package com.hrm.employeemanagement.application.port.outbound.milestone;

import com.hrm.employeemanagement.domain.milestone.MilestoneId;

public interface DeleteMilestonePort {
    void deleteById(MilestoneId id);
}
