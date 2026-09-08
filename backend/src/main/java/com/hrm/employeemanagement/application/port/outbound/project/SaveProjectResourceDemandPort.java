package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;

import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;

public interface SaveProjectResourceDemandPort {

    /**
     * Lưu một bản ghi nhu cầu nhân sự.
     */
    ProjectResourceDemand save(ProjectResourceDemand demand);

    /**
     * Lưu danh sách các bản ghi nhu cầu nhân sự theo đợt (batch save).
     */
    List<ProjectResourceDemand> saveAll(List<ProjectResourceDemand> demands);
}