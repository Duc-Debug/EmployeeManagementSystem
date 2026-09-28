package com.hrm.employeemanagement.application.port.inbound.report.projectallocation;

import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportQuery;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportResult;

public interface GetProjectAllocationReportUseCase {
    ProjectAllocationReportResult execute(ProjectAllocationReportQuery query);
}