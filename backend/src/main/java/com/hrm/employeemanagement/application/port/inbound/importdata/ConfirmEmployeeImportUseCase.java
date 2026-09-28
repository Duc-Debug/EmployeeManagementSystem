package com.hrm.employeemanagement.application.port.inbound.importdata;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;

public interface ConfirmEmployeeImportUseCase {
    ImportExecutionResult confirm(ConfirmEmployeeImportCommand command);
}
