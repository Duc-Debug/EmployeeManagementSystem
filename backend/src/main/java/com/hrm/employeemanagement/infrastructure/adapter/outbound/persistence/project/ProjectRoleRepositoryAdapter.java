package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;

@Component
public class ProjectRoleRepositoryAdapter implements LoadProjectRolePort {

    private final SpringDataProjectRoleRepository springDataProjectRoleRepository;

    public ProjectRoleRepositoryAdapter(SpringDataProjectRoleRepository springDataProjectRoleRepository) {
        this.springDataProjectRoleRepository = springDataProjectRoleRepository;
    }

    @Override
    public List<ProjectRole> findAll() {
        return springDataProjectRoleRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProjectRole> findById(ProjectRoleId id) {
        if (id == null || id.value() == null) {
            return Optional.empty();
        }
        return springDataProjectRoleRepository.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<ProjectRole> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return springDataProjectRoleRepository.findByCode(code.trim().toUpperCase())
                .map(this::toDomain);
    }

    private ProjectRole toDomain(ProjectRoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProjectRole(
                new ProjectRoleId(entity.getId()),
                entity.getCode(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
