package com.hrm.employeemanagement.application.dto.task.comment;

import java.time.LocalDateTime;
import java.util.List;

public record TaskCommentResult(
        Long id,
        Long taskId,
        Long authorId,
        String authorName,
        String authorEmail,
        String authorRole,
        String content,
        List<TaskAttachmentResult> attachments,
        List<MentionedUserDto> mentions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {
}

