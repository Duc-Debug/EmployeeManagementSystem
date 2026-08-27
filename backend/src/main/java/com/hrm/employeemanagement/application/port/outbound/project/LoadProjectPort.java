package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;

public interface LoadProjectPort {
    Optional<Project> findById(ProjectId id);

    List<Project> findAll(int page, int size);

    long count();

    List<Project> findByOrgUnitBranch(Long scopeOrgUnitId, int page, int size);

    long countByOrgUnitBranch(Long scopeOrgUnitId);

    List<Project> findManagedBy(Long employeeId, int page, int size);

    long countManagedBy(Long employeeId);

    List<Project> findMemberProjects(Long employeeId, int page, int size);

    long countMemberProjects(Long employeeId);

    boolean existsInOrgUnitBranch(Long projectId, Long scopeOrgUnitId);

    boolean existsManagedBy(Long projectId, Long employeeId);

    boolean existsMember(Long projectId, Long employeeId);

    boolean existsByProjectCode(String projectCode);
}
