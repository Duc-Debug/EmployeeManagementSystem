package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity.ProjectRoleAllocationTemplateJpaEntity;

@Repository
public interface SpringDataProjectRoleAllocationTemplateRepository extends JpaRepository<ProjectRoleAllocationTemplateJpaEntity, Long> {

    Optional<ProjectRoleAllocationTemplateJpaEntity> findByTemplateCode(String templateCode);

    boolean existsByTemplateCode(String templateCode);

    List<ProjectRoleAllocationTemplateJpaEntity> findAllByOrderByIdDesc();
}

