package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;

@Repository
public interface SpringDataTaskRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

    boolean existsByIdAndProjectId(Long id, Long projectId);

    boolean existsByParentId(Long parentId);

    long countByProjectId(Long projectId);
}
