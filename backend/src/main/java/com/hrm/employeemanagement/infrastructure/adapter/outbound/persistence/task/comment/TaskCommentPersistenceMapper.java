package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachmentId;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskAttachmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskCommentJpaEntity;

@Component
public class TaskCommentPersistenceMapper {

    public TaskComment toDomain(TaskCommentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TaskAttachment> attachments = new ArrayList<>();
        if (entity.getAttachments() != null) {
            for (TaskAttachmentJpaEntity att : entity.getAttachments()) {
                attachments.add(new TaskAttachment(
                        TaskAttachmentId.of(att.getId()),
                        TaskCommentId.of(att.getComment() != null ? att.getComment().getId() : null),
                        TaskId.of(att.getTaskId()),
                        att.getFileName(),
                        att.getFilePath(),
                        att.getFileSize(),
                        att.getFileType(),
                        new UserId(att.getUploadedBy()),
                        att.getUploadedAt()));
            }
        }

        Set<UserId> mentionedUserIds = new HashSet<>();
        if (entity.getMentionedUserIds() != null) {
            for (Long uid : entity.getMentionedUserIds()) {
                mentionedUserIds.add(new UserId(uid));
            }
        }

        return new TaskComment(
                TaskCommentId.of(entity.getId()),
                TaskId.of(entity.getTaskId()),
                new UserId(entity.getAuthorId()),
                entity.getContent(),
                attachments,
                mentionedUserIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public TaskCommentJpaEntity toJpaEntity(TaskComment domain) {
        if (domain == null) {
            return null;
        }

        TaskCommentJpaEntity entity = new TaskCommentJpaEntity(
                domain.getId() != null ? domain.getId().value() : null,
                domain.getTaskId().value(),
                domain.getAuthorId().value(),
                domain.getContent(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion());

        if (domain.getMentionedUserIds() != null) {
            entity.setMentionedUserIds(domain.getMentionedUserIds().stream()
                    .map(UserId::value)
                    .collect(Collectors.toSet()));
        }

        if (domain.getAttachments() != null) {
            List<TaskAttachmentJpaEntity> attEntities = domain.getAttachments().stream().map(att -> {
                TaskAttachmentJpaEntity attEntity = new TaskAttachmentJpaEntity(
                        att.getId() != null ? att.getId().value() : null,
                        entity,
                        att.getTaskId().value(),
                        att.getFileName(),
                        att.getFilePath(),
                        att.getFileSize(),
                        att.getFileType(),
                        att.getUploadedBy().value(),
                        att.getUploadedAt());
                return attEntity;
            }).collect(Collectors.toList());
            entity.setAttachments(attEntities);
        }

        return entity;
    }
}

