package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;

public interface GetProjectResourceDemandUseCase {

    /**
     * Lấy bảng tổng hợp nhu cầu nhân sự của dự án theo từng vai trò và tuần.
     */
    ProjectResourceDemandSummaryResult getProjectResourceDemands(Long projectId);
}