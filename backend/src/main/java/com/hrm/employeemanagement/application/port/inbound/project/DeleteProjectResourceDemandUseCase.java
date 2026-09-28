package com.hrm.employeemanagement.application.port.inbound.project;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;

public interface DeleteProjectResourceDemandUseCase {

    /**
     * Xóa ước lượng nhu cầu nhân sự của một vai trò trong dự án.
     */
    ProjectResourceDemandSummaryResult deleteDemand(Long projectId, Long roleId);
}
