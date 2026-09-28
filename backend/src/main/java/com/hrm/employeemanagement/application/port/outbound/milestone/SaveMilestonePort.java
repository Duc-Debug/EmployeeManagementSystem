package com.hrm.employeemanagement.application.port.outbound.milestone;

import com.hrm.employeemanagement.domain.milestone.Milestone;

public interface SaveMilestonePort {
    Milestone save(Milestone milestone);
}
