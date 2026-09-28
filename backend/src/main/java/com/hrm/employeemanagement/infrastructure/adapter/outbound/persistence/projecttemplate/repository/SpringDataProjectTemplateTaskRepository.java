package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.entity.ProjectTemplateTaskJpaEntity;

public interface SpringDataProjectTemplateTaskRepository extends JpaRepository<ProjectTemplateTaskJpaEntity, Long> {
    List<ProjectTemplateTaskJpaEntity> findByTemplateIdOrderBySortOrderAscIdAsc(Long templateId);
    List<ProjectTemplateTaskJpaEntity> findByTemplateIdInOrderBySortOrderAscIdAsc(java.util.Collection<Long> templateIds);
}
