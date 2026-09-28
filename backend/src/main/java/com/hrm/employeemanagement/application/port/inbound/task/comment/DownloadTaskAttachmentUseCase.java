package com.hrm.employeemanagement.application.port.inbound.task.comment;

import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;

public interface DownloadTaskAttachmentUseCase {
    TaskAttachmentDownloadResult downloadAttachment(Long attachmentId);
}

