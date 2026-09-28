package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;

public interface LoadProjectRolePort {

    List<ProjectRole> findAll();

    List<ProjectRole> findAllActive();

    Optional<ProjectRole> findById(ProjectRoleId id);

    Optional<ProjectRole> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
