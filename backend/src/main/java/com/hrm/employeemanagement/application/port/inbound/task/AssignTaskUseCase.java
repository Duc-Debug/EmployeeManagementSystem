package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.AssignTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskAssignmentResult;

public interface AssignTaskUseCase {

    TaskAssignmentResult assignTask(AssignTaskCommand command);
}

