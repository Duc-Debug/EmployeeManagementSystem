package com.hrm.employeemanagement.application.port.inbound.projecttemplate;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;

public interface CreateProjectFromTemplateUseCase {
    ProjectResult createProjectFromTemplate(CreateProjectFromTemplateCommand command);
}
