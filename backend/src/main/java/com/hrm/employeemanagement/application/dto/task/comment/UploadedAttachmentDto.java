package com.hrm.employeemanagement.application.dto.task.comment;

public record UploadedAttachmentDto(
        String originalFileName,
        String storedFilePath,
        Long fileSize,
        String contentType) {
}

