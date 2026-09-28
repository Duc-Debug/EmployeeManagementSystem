package com.hrm.employeemanagement.application.port.outbound.task;

import java.util.List;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;

public interface SaveTaskAssignmentPort {

    TaskAssignment save(TaskAssignment taskAssignment);

    List<TaskAssignment> saveAll(List<TaskAssignment> taskAssignments);

    void deleteByTaskId(TaskId taskId);

    void deleteByTaskIdAndEmployeeIdNotIn(TaskId taskId, List<EmployeeId> employeeIds);
}

