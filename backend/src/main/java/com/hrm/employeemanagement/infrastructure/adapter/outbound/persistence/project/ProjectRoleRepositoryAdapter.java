package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.CountProjectRoleUsagePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectRolePort;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException;
import com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleStateException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;

@Component
public class ProjectRoleRepositoryAdapter implements
        LoadProjectRolePort,
        SaveProjectRolePort,
        CountProjectRoleUsagePort {

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

        try {
            ProjectRoleJpaEntity saved = springDataProjectRoleRepository.saveAndFlush(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateConstraintViolation(ex, "uk_project_roles_name") || isDuplicateConstraintViolation(ex, "name")) {
                throw new DuplicateProjectRoleNameException(domain.getName());
            }
            if (isDuplicateConstraintViolation(ex, "code")) {
                throw new DuplicateProjectRoleCodeException(domain.getCode());
            }
            throw ex;
        }
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
    public void syncEmployeeProfessionalRole(String oldRoleName, String newRoleName) {
        if (oldRoleName != null && newRoleName != null && !oldRoleName.trim().equalsIgnoreCase(newRoleName.trim())) {
            springDataProjectRoleRepository.syncEmployeeProfessionalRole(oldRoleName.trim(), newRoleName.trim());
        }
    }

    private ProjectRole toDomain(ProjectRoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            throw new InvalidProjectRoleStateException(
                    "Trạng thái vai trò chuyên môn trong CSDL không được để trống (id=" + entity.getId() + ")");
        }
        ProjectRoleStatus status;
        try {
            status = ProjectRoleStatus.valueOf(entity.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectRoleStateException(
                    "Trạng thái vai trò chuyên môn không hợp lệ trong CSDL: '" + entity.getStatus() + "' (id=" + entity.getId() + ")");
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

    private boolean isDuplicateConstraintViolation(DataIntegrityViolationException ex, String constraintOrField) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String cName = cve.getConstraintName();
                if (cName != null && cName.toLowerCase().contains(constraintOrField.toLowerCase())) {
                    return true;
                }
            }
            if (current instanceof java.sql.SQLException sqlEx) {
                String sqlState = sqlEx.getSQLState();
                int errorCode = sqlEx.getErrorCode();
                if ("23505".equals(sqlState) || errorCode == 1062) {
                    String msg = sqlEx.getMessage() != null ? sqlEx.getMessage().toLowerCase() : "";
                    if (msg.contains(constraintOrField.toLowerCase())) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        return rootMsg != null && rootMsg.toLowerCase().contains(constraintOrField.toLowerCase());
    }
}
