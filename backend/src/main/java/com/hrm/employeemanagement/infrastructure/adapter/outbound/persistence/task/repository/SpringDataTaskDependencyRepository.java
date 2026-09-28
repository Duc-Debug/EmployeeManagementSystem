package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskDependencyJpaEntity;

@Repository
public interface SpringDataTaskDependencyRepository extends JpaRepository<TaskDependencyJpaEntity, Long> {
    List<TaskDependencyJpaEntity> findByProjectId(Long projectId);
    List<TaskDependencyJpaEntity> findByProjectIdAndPredecessorId(Long projectId, Long predecessorId);
    List<TaskDependencyJpaEntity> findByProjectIdAndSuccessorId(Long projectId, Long successorId);
    boolean existsByPredecessorIdAndSuccessorId(Long predecessorId, Long successorId);
}
