package com.hrm.employeemanagement.application.port.outbound.project;

import com.hrm.employeemanagement.domain.project.Project;

public interface SaveProjectPort {
    Project save(Project project);
}