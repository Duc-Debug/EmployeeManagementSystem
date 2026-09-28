package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.milestone.entity.ProjectMilestoneJpaEntity;

@Repository
public interface SpringDataProjectMilestoneRepository extends JpaRepository<ProjectMilestoneJpaEntity, Long> {

    List<ProjectMilestoneJpaEntity> findAllByProjectIdOrderByPlannedDateAsc(Long projectId);

    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
