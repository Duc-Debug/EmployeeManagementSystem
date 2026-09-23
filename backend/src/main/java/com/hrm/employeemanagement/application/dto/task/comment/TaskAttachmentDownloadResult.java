package com.hrm.employeemanagement.application.dto.task.comment;

import java.io.InputStream;

public record TaskAttachmentDownloadResult(
        String fileName,
        String fileType,
        Long fileSize,
        InputStream contentStream) {
}

