package com.hrm.employeemanagement.application.port.outbound.task;

import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;

public interface SaveTaskPort {
    Task save(Task task);

    void delete(TaskId id);
}