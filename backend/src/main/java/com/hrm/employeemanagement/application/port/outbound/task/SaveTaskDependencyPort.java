package com.hrm.employeemanagement.application.port.outbound.task;

import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;

public interface SaveTaskDependencyPort {
    TaskDependency save(TaskDependency dependency);
}
