package com.hrm.employeemanagement.domain.projecttemplate;

public record ProjectTemplateTaskId(Long value) {
    public ProjectTemplateTaskId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ProjectTemplateTaskId value phải lớn hơn 0");
        }
    }
}