package com.hrm.employeemanagement.application.port.inbound.task.comment;

import java.util.List;

import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;

public interface GetTaskCommentsUseCase {
    List<TaskCommentResult> execute(Long taskId);
}

