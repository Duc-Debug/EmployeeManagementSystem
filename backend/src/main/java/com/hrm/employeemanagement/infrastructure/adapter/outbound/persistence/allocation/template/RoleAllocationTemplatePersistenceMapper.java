package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplateItem;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity.ProjectRoleAllocationTemplateItemJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity.ProjectRoleAllocationTemplateJpaEntity;

@Component
public class RoleAllocationTemplatePersistenceMapper {

    public ProjectRoleAllocationTemplateJpaEntity toJpaEntity(ProjectRoleAllocationTemplate domain) {
        if (domain == null) {
            return null;
        }

        ProjectRoleAllocationTemplateJpaEntity entity = new ProjectRoleAllocationTemplateJpaEntity(
                domain.getId(),
                domain.getTemplateCode(),
                domain.getName(),
                domain.getDescription(),
                domain.getSourceProjectId(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );

        if (domain.getItems() != null) {
            for (ProjectRoleAllocationTemplateItem item : domain.getItems()) {
                ProjectRoleAllocationTemplateItemJpaEntity itemEntity = new ProjectRoleAllocationTemplateItemJpaEntity(
                        item.getId(),
                        entity,
                        item.getRoleId(),
                        item.getHoursPerWeek(),
                        null
                );
                entity.addItem(itemEntity);
            }
        }

        return entity;
    }

    public ProjectRoleAllocationTemplate toDomain(ProjectRoleAllocationTemplateJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<ProjectRoleAllocationTemplateItem> items = null;
        if (entity.getItems() != null) {
            items = entity.getItems().stream()
                    .map(itemEntity -> new ProjectRoleAllocationTemplateItem(
                            itemEntity.getId(),
                            entity.getId(),
                            itemEntity.getRoleId(),
                            itemEntity.getHoursPerWeek()
                    ))
                    .collect(Collectors.toList());
        }

        return new ProjectRoleAllocationTemplate(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getSourceProjectId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                items
        );
    }
}

