package com.hrm.employeemanagement.application.port.inbound.task.comment;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;

public interface CreateTaskCommentUseCase {
    TaskCommentResult execute(CreateTaskCommentCommand command);
}

