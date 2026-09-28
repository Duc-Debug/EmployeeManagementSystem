package com.hrm.employeemanagement.domain.task.comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.hrm.employeemanagement.domain.exception.task.InvalidCommentDataException;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskComment {
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final TaskCommentId id;
    private final TaskId taskId;
    private final UserId authorId;
    private String content;
    private final List<TaskAttachment> attachments;
    private final Set<UserId> mentionedUserIds;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public TaskComment(
            TaskCommentId id,
            TaskId taskId,
            UserId authorId,
            String content,
            List<TaskAttachment> attachments,
            Set<UserId> mentionedUserIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        this.id = id;
        this.taskId = Objects.requireNonNull(taskId, "TaskId không được null");
        this.authorId = Objects.requireNonNull(authorId, "AuthorId không được null");
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        validateContent(content, this.attachments);
        this.content = (content != null) ? content.trim() : "";
        this.mentionedUserIds = mentionedUserIds != null ? new HashSet<>(mentionedUserIds) : new HashSet<>();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static TaskComment create(
            TaskId taskId,
            UserId authorId,
            String content,
            Set<UserId> mentionedUserIds) {
        return new TaskComment(
                null,
                taskId,
                authorId,
                content,
                new ArrayList<>(),
                mentionedUserIds,
                LocalDateTime.now(),
                null,
                0L);
    }

    private void validateContent(String content, List<TaskAttachment> attachments) {
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new InvalidCommentDataException("Vui lòng nhập nội dung trao đổi hoặc đính kèm ít nhất một tệp");
        }
        if (content != null && content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new InvalidCommentDataException("Nội dung ghi chú không được vượt quá " + MAX_CONTENT_LENGTH + " ký tự");
        }
    }

    public void updateContent(String newContent) {
        validateContent(newContent, this.attachments);
        this.content = (newContent != null) ? newContent.trim() : "";
        this.updatedAt = LocalDateTime.now();
    }

    public void addAttachment(TaskAttachment attachment) {
        if (attachment != null) {
            this.attachments.add(attachment);
        }
    }

    public void addMention(UserId userId) {
        if (userId != null && !userId.equals(this.authorId)) {
            this.mentionedUserIds.add(userId);
        }
    }

    public TaskCommentId getId() {
        return id;
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public UserId getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public List<TaskAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public Set<UserId> getMentionedUserIds() {
        return Collections.unmodifiableSet(mentionedUserIds);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}

