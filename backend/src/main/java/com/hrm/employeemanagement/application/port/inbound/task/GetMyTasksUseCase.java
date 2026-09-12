package com.hrm.employeemanagement.application.port.inbound.task;

import java.util.List;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;

public interface GetMyTasksUseCase {

    List<MyTaskResult> getMyTasks();
}

