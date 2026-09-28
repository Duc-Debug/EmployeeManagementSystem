package com.hrm.employeemanagement.domain.projecttemplate;

public record ProjectTemplateId(Long value) {
    public ProjectTemplateId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ProjectTemplateId value phải lớn hơn 0");
        }
    }
}