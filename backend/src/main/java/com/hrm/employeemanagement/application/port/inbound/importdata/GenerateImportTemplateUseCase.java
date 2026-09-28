package com.hrm.employeemanagement.application.port.inbound.importdata;

public interface GenerateImportTemplateUseCase {
    byte[] generateEmployeeTemplate(String format);
}
