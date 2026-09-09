package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTaskId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.entity.ProjectTemplateJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.entity.ProjectTemplateTaskJpaEntity;

@Component
public class ProjectTemplatePersistenceMapper {

    public ProjectTemplate toDomain(ProjectTemplateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProjectTemplate(
                new ProjectTemplateId(entity.getId()),
                entity.getTemplateCode(),
                entity.getName(),
                entity.getDescription(),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getCreatedBy() != null ? new UserId(entity.getCreatedBy()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public ProjectTemplateTask toDomain(ProjectTemplateTaskJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProjectTemplateTask(
                new ProjectTemplateTaskId(entity.getId()),
                new ProjectTemplateId(entity.getTemplateId()),
                entity.getParentId() != null ? new ProjectTemplateTaskId(entity.getParentId()) : null,
                entity.getName(),
                entity.getDescription(),
                entity.getTaskType(),
                entity.getEstimatedHours(),
                entity.getSortOrder(),
                entity.getCreatedAt());
    }
}
