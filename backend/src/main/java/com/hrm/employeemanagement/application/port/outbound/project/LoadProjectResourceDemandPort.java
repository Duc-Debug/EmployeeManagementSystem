package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.role.RoleId;

public interface LoadProjectResourceDemandPort {

    /**
     * Lấy toàn bộ danh sách nhu cầu nhân sự của dự án.
     */
    List<ProjectResourceDemand> findByProjectId(ProjectId projectId);

    /**
     * Lấy danh sách nhu cầu nhân sự của dự án lọc theo vai trò chuyên môn.
     */
    List<ProjectResourceDemand> findByProjectIdAndRoleId(ProjectId projectId, RoleId roleId);

    /**
     * Lấy bản ghi nhu cầu của dự án theo vai trò và tuần cụ thể.
     */
    Optional<ProjectResourceDemand> findByProjectIdAndRoleIdAndYearWeek(
            ProjectId projectId, RoleId roleId, YearWeek yearWeek);
}