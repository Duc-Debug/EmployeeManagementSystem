package com.hrm.employeemanagement.application.port.inbound.milestone;

public interface DeleteMilestoneUseCase {
    void deleteMilestone(Long projectId, Long milestoneId);
}
