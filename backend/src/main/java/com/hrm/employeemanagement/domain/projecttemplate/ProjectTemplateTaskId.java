package com.hrm.employeemanagement.domain.projecttemplate;

import java.util.Objects;

public record ProjectTemplateTaskId(Long value) {
    public ProjectTemplateTaskId {
        Objects.requireNonNull(value, "ProjectTemplateTaskId value must not be null");
    }
}