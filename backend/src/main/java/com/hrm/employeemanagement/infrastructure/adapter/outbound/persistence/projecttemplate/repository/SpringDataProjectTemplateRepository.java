package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.entity.ProjectTemplateJpaEntity;

public interface SpringDataProjectTemplateRepository extends JpaRepository<ProjectTemplateJpaEntity, Long> {
    List<ProjectTemplateJpaEntity> findByActiveTrueOrderByIdAsc();
    java.util.Optional<ProjectTemplateJpaEntity> findByIdAndActiveTrue(Long id);
    java.util.Optional<ProjectTemplateJpaEntity> findByTemplateCode(String templateCode);
}
