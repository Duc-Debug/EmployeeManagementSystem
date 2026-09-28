package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskAttachmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskCommentJpaEntity;

class TaskCommentPersistenceMapperTest {

    private final TaskCommentPersistenceMapper mapper = new TaskCommentPersistenceMapper();

    @Test
    void toDomainMapsEachAttachmentExactlyOnce() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 14, 10, 0);
        TaskCommentJpaEntity commentEntity = new TaskCommentJpaEntity(
                10L, 20L, 30L, "Review design", createdAt, null, 0L);
        TaskAttachmentJpaEntity attachmentEntity = new TaskAttachmentJpaEntity(
                15L,
                commentEntity,
                20L,
                "design.pdf",
                "/uploads/design.pdf",
                1024L,
                "application/pdf",
                30L,
                createdAt);
        commentEntity.setAttachments(List.of(attachmentEntity));

        TaskComment mapped = mapper.toDomain(commentEntity);

        assertEquals(1, mapped.getAttachments().size());
        assertEquals(15L, mapped.getAttachments().getFirst().getId().value());
        assertEquals("/uploads/design.pdf", mapped.getAttachments().getFirst().getFilePath());
    }
}
