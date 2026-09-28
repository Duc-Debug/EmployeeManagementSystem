package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;

public interface EstimateResourceDemandUseCase {

    /**
     * Ước lượng nhu cầu nhân sự theo vai trò cho toàn bộ các tuần của dự án (NCL-03-CN-007).
     */
    ProjectResourceDemandSummaryResult estimateDemand(EstimateResourceDemandCommand command);
}