package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.CreateTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskResult;

public interface CreateTaskUseCase {
    TaskResult createTask(CreateTaskCommand command);
}