package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;

public interface LoadProjectMemberPort {
    List<ProjectMemberResult> findMembersByProjectId(Long projectId);
    boolean existsMember(Long projectId, Long employeeId);
    boolean hasActiveTasks(Long projectId, Long employeeId);
}
