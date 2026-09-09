package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository.SpringDataProjectTemplateRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository.SpringDataProjectTemplateTaskRepository;

@Component
public class ProjectTemplateRepositoryAdapter implements LoadProjectTemplatePort {

    private final SpringDataProjectTemplateRepository templateRepository;
    private final SpringDataProjectTemplateTaskRepository taskRepository;
    private final ProjectTemplatePersistenceMapper mapper;

    public ProjectTemplateRepositoryAdapter(
            SpringDataProjectTemplateRepository templateRepository,
            SpringDataProjectTemplateTaskRepository taskRepository,
            ProjectTemplatePersistenceMapper mapper) {
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<ProjectTemplate> findById(ProjectTemplateId templateId) {
        if (templateId == null || templateId.value() == null) {
            return Optional.empty();
        }
        return templateRepository.findById(templateId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<ProjectTemplate> findAllActive() {
        return templateRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProjectTemplateTask> findTasksByTemplateId(ProjectTemplateId templateId) {
        if (templateId == null || templateId.value() == null) {
            return List.of();
        }
        return taskRepository.findByTemplateIdOrderBySortOrderAscIdAsc(templateId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
