package com.hrm.employeemanagement.domain.projecttemplate;

import java.util.Objects;

public record ProjectTemplateId(Long value) {
    public ProjectTemplateId {
        Objects.requireNonNull(value, "ProjectTemplateId value must not be null");
    }
}