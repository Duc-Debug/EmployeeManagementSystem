package com.hrm.employeemanagement.application.port.inbound.timesheet;

import java.util.List;

import com.hrm.employeemanagement.application.dto.timesheet.AssignedTaskOptionResult;

public interface GetMyAssignedTasksForWorkLogUseCase {
    List<AssignedTaskOptionResult> getMyAssignedTasksForWorkLog();
}
