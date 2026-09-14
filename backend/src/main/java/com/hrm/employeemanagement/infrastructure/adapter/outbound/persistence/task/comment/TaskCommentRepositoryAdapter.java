package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.task.comment.DeleteTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.SaveTaskCommentPort;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskCommentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository.SpringDataTaskCommentRepository;

@Component
public class TaskCommentRepositoryAdapter
        implements LoadTaskCommentPort, SaveTaskCommentPort, DeleteTaskCommentPort {

    private final SpringDataTaskCommentRepository commentRepository;
    private final TaskCommentPersistenceMapper mapper;

    public TaskCommentRepositoryAdapter(
            SpringDataTaskCommentRepository commentRepository,
            TaskCommentPersistenceMapper mapper) {
        this.commentRepository = Objects.requireNonNull(commentRepository, "SpringDataTaskCommentRepository không được null");
        this.mapper = Objects.requireNonNull(mapper, "TaskCommentPersistenceMapper không được null");
    }

    @Override
    public Optional<TaskComment> findById(TaskCommentId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return commentRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<TaskComment> findAllByTaskId(TaskId taskId) {
        if (taskId == null || taskId.value() == null) {
            return List.of();
        }
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public TaskComment save(TaskComment comment) {
        TaskCommentJpaEntity entity = mapper.toJpaEntity(comment);
        TaskCommentJpaEntity saved = commentRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(TaskCommentId id) {
        if (id != null && id.value() != null) {
            commentRepository.deleteById(id.value());
        }
    }
}

