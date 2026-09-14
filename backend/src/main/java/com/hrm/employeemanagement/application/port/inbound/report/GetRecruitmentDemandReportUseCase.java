package com.hrm.employeemanagement.application.port.inbound.report;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportQuery;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;

public interface GetRecruitmentDemandReportUseCase {
    RecruitmentDemandReportResult execute(RecruitmentDemandReportQuery query);
}
