package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectMemberJpaEntity;

@Repository
public interface SpringDataProjectMemberRepository
        extends JpaRepository<
                ProjectMemberJpaEntity,
                ProjectMemberJpaEntity.ProjectMemberJpaId> {
}
