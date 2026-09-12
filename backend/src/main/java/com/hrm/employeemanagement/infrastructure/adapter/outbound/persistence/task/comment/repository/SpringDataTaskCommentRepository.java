package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskCommentJpaEntity;

@Repository
public interface SpringDataTaskCommentRepository extends JpaRepository<TaskCommentJpaEntity, Long> {

    List<TaskCommentJpaEntity> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    void deleteByTaskId(Long taskId);
}

