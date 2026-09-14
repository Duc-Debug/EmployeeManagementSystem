package com.hrm.employeemanagement.application.port.inbound.report;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportExport;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportQuery;

public interface ExportRecruitmentDemandReportUseCase {
    RecruitmentDemandReportExport export(RecruitmentDemandReportQuery query);
}
