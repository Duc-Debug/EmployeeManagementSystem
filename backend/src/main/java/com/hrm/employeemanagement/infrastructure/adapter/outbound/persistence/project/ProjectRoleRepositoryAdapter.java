package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.CountProjectRoleUsagePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SyncEmployeeProfessionalRolePort;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;

@Component
public class ProjectRoleRepositoryAdapter implements
        LoadProjectRolePort,
        SaveProjectRolePort,
        CountProjectRoleUsagePort,
        SyncEmployeeProfessionalRolePort {

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
    public List<ProjectRole> findAllActive() {
        return springDataProjectRoleRepository.findByStatus("ACTIVE").stream()
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

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        return code != null && springDataProjectRoleRepository.existsByCodeIgnoreCase(code.trim());
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return name != null && springDataProjectRoleRepository.existsByNameIgnoreCase(name.trim());
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
        return name != null && springDataProjectRoleRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id);
    }

    @Override
    public ProjectRole save(ProjectRole domain) {
        if (domain == null) return null;
        ProjectRoleJpaEntity entity;
        if (domain.getId() != null && domain.getIdValue() != null) {
            entity = springDataProjectRoleRepository.findById(domain.getIdValue())
                    .orElseGet(ProjectRoleJpaEntity::new);
        } else {
            entity = new ProjectRoleJpaEntity();
        }
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setSkillGroupId(domain.getSkillGroupId());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE");

        ProjectRoleJpaEntity saved = springDataProjectRoleRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public long countDemandsByRoleId(Long roleId) {
        return roleId != null ? springDataProjectRoleRepository.countDemandsByRoleId(roleId) : 0L;
    }

    @Override
    public long countEmployeesByProfessionalRole(String roleName, String roleCode) {
        String rName = roleName != null ? roleName.trim() : "";
        String rCode = roleCode != null ? roleCode.trim() : "";
        return springDataProjectRoleRepository.countEmployeesByProfessionalRole(rName, rCode);
    }

    @Override
    public int syncRoleName(String oldRoleName, String newRoleName) {
        if (oldRoleName == null || newRoleName == null || oldRoleName.trim().equalsIgnoreCase(newRoleName.trim())) {
            return 0;
        }
        return springDataProjectRoleRepository.updateEmployeeProfessionalRoleName(oldRoleName.trim(), newRoleName.trim());
    }

    private ProjectRole toDomain(ProjectRoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        ProjectRoleStatus status = ProjectRoleStatus.ACTIVE;
        if (entity.getStatus() != null) {
            try {
                status = ProjectRoleStatus.valueOf(entity.getStatus().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                status = ProjectRoleStatus.ACTIVE;
            }
        }
        return new ProjectRole(
                new ProjectRoleId(entity.getId()),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getSkillGroupId(),
                null,
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
