package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;

public interface MoveTaskBoardStatusUseCase {

    TaskBoardCardResult moveTaskStatus(MoveTaskBoardStatusCommand command);
}
