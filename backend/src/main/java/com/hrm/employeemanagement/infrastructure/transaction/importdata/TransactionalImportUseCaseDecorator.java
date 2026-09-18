package com.hrm.employeemanagement.infrastructure.transaction.importdata;

import java.io.InputStream;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.inbound.importdata.ConfirmEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.GenerateImportTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.PreviewEmployeeImportUseCase;

public class TransactionalImportUseCaseDecorator implements
        PreviewEmployeeImportUseCase,
        ConfirmEmployeeImportUseCase,
        GenerateImportTemplateUseCase {

    private final PreviewEmployeeImportUseCase previewUseCase;
    private final ConfirmEmployeeImportUseCase confirmUseCase;
    private final GenerateImportTemplateUseCase generateTemplateUseCase;

    public TransactionalImportUseCaseDecorator(
            PreviewEmployeeImportUseCase previewUseCase,
            ConfirmEmployeeImportUseCase confirmUseCase,
            GenerateImportTemplateUseCase generateTemplateUseCase
    ) {
        this.previewUseCase = previewUseCase;
        this.confirmUseCase = confirmUseCase;
        this.generateTemplateUseCase = generateTemplateUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportEmployeePreviewResult preview(InputStream inputStream, String fileName) {
        return previewUseCase.preview(inputStream, fileName);
    }

    @Override
    @Transactional
    public ImportExecutionResult confirm(ConfirmEmployeeImportCommand command) {
        return confirmUseCase.confirm(command);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateEmployeeTemplate(String format) {
        return generateTemplateUseCase.generateEmployeeTemplate(format);
    }
}
