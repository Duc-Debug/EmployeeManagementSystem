package com.hrm.employeemanagement.application.port.inbound.task.comment;

public interface DeleteTaskCommentUseCase {
    void execute(Long taskId, Long commentId, Long requestingUserId);
}

