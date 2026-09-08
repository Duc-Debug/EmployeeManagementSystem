package com.hrm.employeemanagement.application.port.inbound.task;

import java.util.List;

import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;

public interface GetProjectWbsUseCase {
    List<TaskNodeResult> getProjectWbs(Long projectId);
}