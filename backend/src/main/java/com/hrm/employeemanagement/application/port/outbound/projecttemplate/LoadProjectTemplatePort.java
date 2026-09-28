package com.hrm.employeemanagement.application.port.outbound.projecttemplate;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;

public interface LoadProjectTemplatePort {
    Optional<ProjectTemplate> findById(ProjectTemplateId templateId);

    Optional<ProjectTemplate> findActiveById(ProjectTemplateId templateId);

    List<ProjectTemplate> findAllActive();

    List<ProjectTemplateTask> findTasksByTemplateId(ProjectTemplateId templateId);

    List<ProjectTemplateTask> findTasksByTemplateIds(List<ProjectTemplateId> templateIds);
}