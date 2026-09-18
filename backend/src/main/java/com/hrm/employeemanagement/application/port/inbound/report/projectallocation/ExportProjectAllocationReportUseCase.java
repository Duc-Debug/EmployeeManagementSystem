package com.hrm.employeemanagement.application.port.inbound.report.projectallocation;

import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportExport;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportQuery;

public interface ExportProjectAllocationReportUseCase {
    ProjectAllocationReportExport export(ProjectAllocationReportQuery query);
}