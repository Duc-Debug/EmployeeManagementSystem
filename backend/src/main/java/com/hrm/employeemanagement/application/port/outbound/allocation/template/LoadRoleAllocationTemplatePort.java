package com.hrm.employeemanagement.application.port.outbound.allocation.template;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;

public interface LoadRoleAllocationTemplatePort {
    Optional<ProjectRoleAllocationTemplate> findById(Long id);
    Optional<ProjectRoleAllocationTemplate> findByCode(String code);
    boolean existsByCode(String code);
    List<ProjectRoleAllocationTemplate> findAll();
}

