package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskAttachmentPort;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachmentId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository.SpringDataTaskAttachmentRepository;

@Component
public class TaskAttachmentRepositoryAdapter implements LoadTaskAttachmentPort {

    private final SpringDataTaskAttachmentRepository attachmentRepository;
    private final TaskCommentPersistenceMapper mapper;

    public TaskAttachmentRepositoryAdapter(
            SpringDataTaskAttachmentRepository attachmentRepository,
            TaskCommentPersistenceMapper mapper) {
        this.attachmentRepository = Objects.requireNonNull(attachmentRepository, "SpringDataTaskAttachmentRepository không được null");
        this.mapper = Objects.requireNonNull(mapper, "TaskCommentPersistenceMapper không được null");
    }

    @Override
    public Optional<TaskAttachment> findById(TaskAttachmentId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return attachmentRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        return attachmentRepository.existsByFilePath(filePath);
    }
}

