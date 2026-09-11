package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;

@Repository
public interface SpringDataProjectRoleRepository extends JpaRepository<ProjectRoleJpaEntity, Long> {

    Optional<ProjectRoleJpaEntity> findByCode(String code);
}
