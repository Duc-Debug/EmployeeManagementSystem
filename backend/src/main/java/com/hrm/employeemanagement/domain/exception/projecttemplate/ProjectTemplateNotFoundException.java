package com.hrm.employeemanagement.domain.exception.projecttemplate;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectTemplateNotFoundException extends DomainException {
    public ProjectTemplateNotFoundException(Long templateId) {
        super("Không tìm thấy mẫu dự án với ID: " + templateId);
    }

    public ProjectTemplateNotFoundException(String message) {
        super(message);
    }
}
