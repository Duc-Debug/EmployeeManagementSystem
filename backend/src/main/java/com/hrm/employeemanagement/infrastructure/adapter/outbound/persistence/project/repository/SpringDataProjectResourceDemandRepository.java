package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;

@Repository
public interface SpringDataProjectResourceDemandRepository
        extends JpaRepository<ProjectResourceDemandJpaEntity, Long> {

    List<ProjectResourceDemandJpaEntity> findByProjectId(Long projectId);

    List<ProjectResourceDemandJpaEntity> findByProjectIdAndRoleId(Long projectId, Long roleId);

    Optional<ProjectResourceDemandJpaEntity> findByProjectIdAndRoleIdAndYearAndWeekNumber(
            Long projectId, Long roleId, Integer year, Integer weekNumber);
}