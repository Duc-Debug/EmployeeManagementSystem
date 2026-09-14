package com.hrm.employeemanagement.application.dto.task.comment;

import java.util.List;
import java.util.Set;

public record CreateTaskCommentCommand(
        Long taskId,
        Long authorId,
        String content,
        Set<Long> mentionedUserIds,
        List<UploadedAttachmentDto> attachments) {
}

