package com.hrm.employeemanagement.application.service.project;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateSummaryResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateTaskResult;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.GetProjectTemplatesUseCase;
import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.domain.task.TaskType;

public class GetProjectTemplatesService implements GetProjectTemplatesUseCase {

    private final LoadProjectTemplatePort loadProjectTemplatePort;

    public GetProjectTemplatesService(LoadProjectTemplatePort loadProjectTemplatePort) {
        this.loadProjectTemplatePort = Objects.requireNonNull(
                loadProjectTemplatePort, "LoadProjectTemplatePort không được để trống");
    }

    @Override
    public List<ProjectTemplateSummaryResult> getActiveTemplates() {
        List<ProjectTemplate> activeTemplates = loadProjectTemplatePort.findAllActive();

        return activeTemplates.stream().map(template -> {
            List<ProjectTemplateTask> tasks = loadProjectTemplatePort.findTasksByTemplateId(template.getId());

            BigDecimal totalHours = tasks.stream()
                    .map(ProjectTemplateTask::getEstimatedHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int categoriesCount = (int) tasks.stream()
                    .filter(t -> t.getTaskType() == TaskType.CATEGORY)
                    .count();

            int tasksCount = (int) tasks.stream()
                    .filter(t -> t.getTaskType() == TaskType.TASK)
                    .count();

            return new ProjectTemplateSummaryResult(
                    template.getIdValue(),
                    template.getTemplateCode(),
                    template.getName(),
                    template.getDescription(),
                    template.isActive(),
                    totalHours,
                    categoriesCount,
                    tasksCount);
        }).toList();
    }

    @Override
    public ProjectTemplateDetailResult getTemplateDetail(Long templateId) {
        if (templateId == null) {
            throw new InvalidProjectDataException("Mã mẫu dự án không được để trống");
        }

        ProjectTemplateId domainId = new ProjectTemplateId(templateId);
        ProjectTemplate template = loadProjectTemplatePort.findById(domainId)
                .orElseThrow(() -> new ProjectTemplateNotFoundException(templateId));

        List<ProjectTemplateTask> tasks = loadProjectTemplatePort.findTasksByTemplateId(domainId);

        BigDecimal totalHours = tasks.stream()
                .map(ProjectTemplateTask::getEstimatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int categoriesCount = (int) tasks.stream()
                .filter(t -> t.getTaskType() == TaskType.CATEGORY)
                .count();

        int tasksCount = (int) tasks.stream()
                .filter(t -> t.getTaskType() == TaskType.TASK)
                .count();

        List<ProjectTemplateTaskResult> taskResults = tasks.stream().map(t -> new ProjectTemplateTaskResult(
                t.getIdValue(),
                t.getParentIdValue(),
                t.getName(),
                t.getDescription(),
                t.getTaskType() != null ? t.getTaskType().name() : TaskType.TASK.name(),
                t.getEstimatedHours() != null ? t.getEstimatedHours() : BigDecimal.ZERO,
                t.getSortOrder() != null ? t.getSortOrder() : 0)).toList();

        return new ProjectTemplateDetailResult(
                template.getIdValue(),
                template.getTemplateCode(),
                template.getName(),
                template.getDescription(),
                template.isActive(),
                totalHours,
                categoriesCount,
                tasksCount,
                taskResults);
    }
}
