package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskCommand;

/**
 * Inbound port cho Use Case cập nhật thông tin Hạng mục / Công việc trong cây WBS.
 */
public interface UpdateTaskUseCase {
    TaskResult updateTask(UpdateTaskCommand command);
}
