package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectMemberJpaEntity;

@Repository
public interface SpringDataProjectMemberRepository
        extends JpaRepository<
                ProjectMemberJpaEntity,
                ProjectMemberJpaEntity.ProjectMemberJpaId> {

    List<ProjectMemberJpaEntity> findByProjectId(Long projectId);

    boolean existsByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    @Modifying
    @Query("DELETE FROM ProjectMemberJpaEntity pm WHERE pm.projectId = :projectId AND pm.employeeId = :employeeId")
    void deleteByProjectIdAndEmployeeId(@Param("projectId") Long projectId, @Param("employeeId") Long employeeId);
}
