package com.hrm.employeemanagement.application.dto.task.comment;

import java.time.LocalDateTime;

public record TaskAttachmentResult(
        Long id,
        Long commentId,
        Long taskId,
        String fileName,
        String fileDownloadUrl,
        Long fileSize,
        String fileType,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt) {
}

