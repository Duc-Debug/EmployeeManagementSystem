package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.SetTaskBudgetCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBudgetResult;

/**
 * Inbound Port cho Use Case: Đặt ngân sách giờ công cho công việc.
 */
public interface SetTaskBudgetUseCase {
    TaskBudgetResult setTaskBudget(SetTaskBudgetCommand command);
}
