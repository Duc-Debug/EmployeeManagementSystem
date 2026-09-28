package com.hrm.employeemanagement.application.port.outbound.task.comment;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;

public interface LoadTaskCommentPort {
    Optional<TaskComment> findById(TaskCommentId id);

    List<TaskComment> findAllByTaskId(TaskId taskId);
}

