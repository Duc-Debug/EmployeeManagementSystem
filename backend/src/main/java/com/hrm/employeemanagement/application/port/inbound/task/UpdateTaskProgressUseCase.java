package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;

public interface UpdateTaskProgressUseCase {

    TaskProgressResult updateProgress(UpdateTaskProgressCommand command);
}
