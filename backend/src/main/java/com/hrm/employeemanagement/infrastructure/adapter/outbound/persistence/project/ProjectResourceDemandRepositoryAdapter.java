package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;

@Component
public class ProjectResourceDemandRepositoryAdapter implements
        LoadProjectResourceDemandPort,
        SaveProjectResourceDemandPort {

    private final SpringDataProjectResourceDemandRepository repository;
    private final ProjectResourceDemandPersistenceMapper mapper;

    public ProjectResourceDemandRepositoryAdapter(
            SpringDataProjectResourceDemandRepository repository,
            ProjectResourceDemandPersistenceMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "SpringDataProjectResourceDemandRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "ProjectResourceDemandPersistenceMapper must not be null");
    }

    @Override
    public List<ProjectResourceDemand> findByProjectId(ProjectId projectId) {
        if (projectId == null || projectId.value() == null) {
            return Collections.emptyList();
        }
        return repository.findByProjectId(projectId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProjectResourceDemand> findByProjectIdAndRoleId(ProjectId projectId, RoleId roleId) {
        if (projectId == null || roleId == null || projectId.value() == null || roleId.value() == null) {
            return Collections.emptyList();
        }
        return repository.findByProjectIdAndRoleId(projectId.value(), roleId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProjectResourceDemand> findByProjectIdAndRoleIdAndYearWeek(
            ProjectId projectId, RoleId roleId, YearWeek yearWeek) {
        if (projectId == null || roleId == null || yearWeek == null) {
            return Optional.empty();
        }
        return repository.findByProjectIdAndRoleIdAndYearAndWeekNumber(
                projectId.value(), roleId.value(), yearWeek.year(), yearWeek.weekNumber())
                .map(mapper::toDomain);
    }

    @Override
    public ProjectResourceDemand save(ProjectResourceDemand demand) {
        if (demand == null) {
            return null;
        }
        try {
            ProjectResourceDemandJpaEntity entity = mapper.toJpaEntity(demand);
            ProjectResourceDemandJpaEntity saved = repository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateDemandConstraintViolation(ex)) {
                throw new DuplicateResourceDemandException(
                        "Xung đột dữ liệu: Nhu cầu nhân sự cho vai trò và tuần này đã tồn tại hoặc được tạo đồng thời.");
            }
            throw ex;
        }
    }

    @Override
    public List<ProjectResourceDemand> saveAll(List<ProjectResourceDemand> demands) {
        if (demands == null || demands.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<ProjectResourceDemandJpaEntity> entities = demands.stream()
                    .map(mapper::toJpaEntity)
                    .toList();
            List<ProjectResourceDemandJpaEntity> saved = repository.saveAllAndFlush(entities);
            return saved.stream().map(mapper::toDomain).toList();
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateDemandConstraintViolation(ex)) {
                throw new DuplicateResourceDemandException(
                        "Xung đột dữ liệu: Nhu cầu nhân sự cho vai trò và tuần này đã tồn tại hoặc được tạo đồng thời.");
            }
            throw ex;
        }
    }

    @Override
    public void deleteAll(List<ProjectResourceDemand> demands) {
        if (demands == null || demands.isEmpty()) {
            return;
        }
        List<ProjectResourceDemandJpaEntity> entities = demands.stream()
                .map(mapper::toJpaEntity)
                .toList();
        repository.deleteAll(entities);
    }

    private boolean isDuplicateDemandConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";

        if (message.contains("uk_proj_res_demand_proj_role_week")) {
            return true;
        }

        if (cause instanceof java.sql.SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();
            String sqlState = sqlEx.getSQLState();
            if (errorCode == 1062 || "23000".equals(sqlState) || "23505".equals(sqlState)) {
                return message.contains("project_resource_demands") || message.contains("uk_proj_res_demand");
            }
        }
        return false;
    }
}