package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleSkillJpaEntity;

public interface SpringDataProjectRoleSkillRepository extends JpaRepository<ProjectRoleSkillJpaEntity, Long> {
    @Query("""
        SELECT rs FROM ProjectRoleSkillJpaEntity rs
        JOIN SkillJpaEntity s ON s.id = rs.skillId
        JOIN ProjectRoleJpaEntity r ON r.id = rs.roleId
        WHERE rs.roleId IN :roleIds
          AND UPPER(rs.status) = 'ACTIVE'
          AND UPPER(s.status) = 'ACTIVE'
          AND UPPER(r.status) = 'ACTIVE'
    """)
    List<ProjectRoleSkillJpaEntity> findActiveMappingsByRoleIdIn(@Param("roleIds") List<Long> roleIds);
}
