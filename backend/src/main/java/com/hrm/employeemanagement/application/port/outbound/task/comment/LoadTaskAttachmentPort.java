package com.hrm.employeemanagement.application.port.outbound.task.comment;

import java.util.Optional;

import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachmentId;

public interface LoadTaskAttachmentPort {
    Optional<TaskAttachment> findById(TaskAttachmentId id);
}

