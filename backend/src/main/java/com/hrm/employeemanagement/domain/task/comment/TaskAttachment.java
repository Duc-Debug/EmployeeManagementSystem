package com.hrm.employeemanagement.domain.task.comment;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskAttachment {
    private final TaskAttachmentId id;
    private final TaskCommentId commentId;
    private final TaskId taskId;
    private final String fileName;
    private final String filePath;
    private final Long fileSize;
    private final String fileType;
    private final UserId uploadedBy;
    private final LocalDateTime uploadedAt;

    public TaskAttachment(
            TaskAttachmentId id,
            TaskCommentId commentId,
            TaskId taskId,
            String fileName,
            String filePath,
            Long fileSize,
            String fileType,
            UserId uploadedBy,
            LocalDateTime uploadedAt) {
        this.id = id;
        this.commentId = commentId;
        this.taskId = Objects.requireNonNull(taskId, "TaskId không được để trống");
        this.fileName = Objects.requireNonNull(fileName, "Tên file không được để trống").trim();
        this.filePath = Objects.requireNonNull(filePath, "Đường dẫn file không được để trống").trim();
        this.fileSize = Objects.requireNonNull(fileSize, "Kích thước file không được để trống");
        this.fileType = fileType;
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "Người tải lên không được để trống");
        this.uploadedAt = uploadedAt != null ? uploadedAt : LocalDateTime.now();
    }

    public TaskAttachmentId getId() {
        return id;
    }

    public TaskCommentId getCommentId() {
        return commentId;
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public UserId getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}

