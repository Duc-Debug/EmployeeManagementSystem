package com.hrm.employeemanagement.application.port.inbound.importdata;

import java.io.InputStream;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;

public interface PreviewEmployeeImportUseCase {
    ImportEmployeePreviewResult preview(InputStream inputStream, String fileName);
}
