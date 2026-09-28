package com.hrm.employeemanagement.application.port.outbound.milestone;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.ProjectId;

public interface LoadMilestonePort {
    Optional<Milestone> findById(MilestoneId id);

    List<Milestone> findAllByProjectId(ProjectId projectId);

    boolean existsByProjectIdAndName(ProjectId projectId, String name);
}
