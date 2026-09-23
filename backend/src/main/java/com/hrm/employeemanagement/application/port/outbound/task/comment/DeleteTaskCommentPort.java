package com.hrm.employeemanagement.application.port.outbound.task.comment;

import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;

public interface DeleteTaskCommentPort {
    void deleteById(TaskCommentId id);
}

