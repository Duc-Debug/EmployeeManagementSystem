package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;

public interface GetTaskBoardUseCase {

    TaskBoardResult getTaskBoard(TaskBoardQuery query);
}
