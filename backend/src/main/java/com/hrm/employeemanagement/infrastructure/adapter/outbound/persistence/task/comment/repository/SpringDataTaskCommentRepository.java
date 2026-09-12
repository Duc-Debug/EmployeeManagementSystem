package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskCommentJpaEntity;

@Repository
public interface SpringDataTaskCommentRepository extends JpaRepository<TaskCommentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = { "attachments", "mentionedUserIds" })
    Optional<TaskCommentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = { "attachments", "mentionedUserIds" })
    List<TaskCommentJpaEntity> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    void deleteByTaskId(Long taskId);
}

