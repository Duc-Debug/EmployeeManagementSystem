package com.hrm.employeemanagement.application.port.outbound.task.comment;

import com.hrm.employeemanagement.domain.task.comment.TaskComment;

public interface SaveTaskCommentPort {
    TaskComment save(TaskComment comment);
}

