package com.hrm.employeemanagement.application.port.inbound.projecttemplate;

import java.util.List;

import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateSummaryResult;

public interface GetProjectTemplatesUseCase {

    List<ProjectTemplateSummaryResult> getActiveTemplates();

    ProjectTemplateDetailResult getTemplateDetail(Long templateId);
}
