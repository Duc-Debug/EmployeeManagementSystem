package com.hrm.employeemanagement.application.port.outbound.task;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;

public interface LoadTaskAssignmentPort {

    List<TaskAssignment> findByTaskId(TaskId taskId);

    List<TaskAssignment> findByTaskIdIn(List<TaskId> taskIds);

    List<TaskAssignment> findByEmployeeId(EmployeeId employeeId);

    Optional<TaskAssignment> findByTaskIdAndEmployeeId(TaskId taskId, EmployeeId employeeId);
}

