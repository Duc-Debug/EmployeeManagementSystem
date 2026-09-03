package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;

@Component
public class ProjectRepositoryAdapter implements LoadProjectPort, SaveProjectPort {

        private final SpringDataProjectRepository projectRepository;
        private final ProjectPersistenceMapper mapper;

        public ProjectRepositoryAdapter(
                        SpringDataProjectRepository projectRepository,
                        ProjectPersistenceMapper mapper) {
                this.projectRepository = projectRepository;
                this.mapper = mapper;
        }

        @Override
        public Optional<Project> findById(ProjectId id) {
                if (id == null || id.value() == null) {
                        return Optional.empty();
                }

                return projectRepository.findById(id.value())
                                .map(mapper::toDomain);
        }

        @Override
        public List<Project> findAll(
                        int page,
                        int size) {
                return projectRepository
                                .findAllOrdered(size, offset(page, size))
                                .stream()
                                .map(mapper::toDomain)
                                .toList();
        }

        @Override
        public long count() {
                return projectRepository.count();
        }

        @Override
        public List<Project> findByOrgUnitBranch(
                        Long scopeOrgUnitId,
                        int page,
                        int size) {
                return projectRepository
                                .findByOrgUnitBranch(
                                                scopeOrgUnitId,
                                                size,
                                                offset(page, size))
                                .stream()
                                .map(mapper::toDomain)
                                .toList();
        }

        @Override
        public long countByOrgUnitBranch(Long scopeOrgUnitId) {
                return projectRepository.countByOrgUnitBranch(
                                scopeOrgUnitId);
        }

        @Override
        public List<Project> findManagedBy(
                        Long employeeId,
                        int page,
                        int size) {
                return projectRepository
                                .findManagedBy(
                                                employeeId,
                                                size,
                                                offset(page, size))
                                .stream()
                                .map(mapper::toDomain)
                                .toList();
        }

        @Override
        public long countManagedBy(Long employeeId) {
                return projectRepository.countManagedBy(
                                employeeId);
        }

        @Override
        public List<Project> findMemberProjects(
                        Long employeeId,
                        int page,
                        int size) {
                return projectRepository
                                .findMemberProjects(
                                                employeeId,
                                                size,
                                                offset(page, size))
                                .stream()
                                .map(mapper::toDomain)
                                .toList();
        }

        @Override
        public long countMemberProjects(Long employeeId) {
                return projectRepository.countMemberProjects(
                                employeeId);
        }

        @Override
        public boolean existsInOrgUnitBranch(
                        Long projectId,
                        Long scopeOrgUnitId) {
                return projectRepository.existsInOrgUnitBranch(
                                projectId,
                                scopeOrgUnitId);
        }

        @Override
        public boolean existsManagedBy(
                        Long projectId,
                        Long employeeId) {
                return projectRepository.existsManagedBy(
                                projectId,
                                employeeId);
        }

        @Override
        public boolean existsMember(
                        Long projectId,
                        Long employeeId) {
                return projectRepository.existsMember(
                                projectId,
                                employeeId);
        }

        private int offset(
                        int page,
                        int size) {
                return page * size;
        }

        @Override
        public Project save(Project project) {
                try {
                        ProjectJpaEntity entity = mapper.toJpaEntity(project);
                        ProjectJpaEntity savedEntity = projectRepository.saveAndFlush(entity);
                        return mapper.toDomain(savedEntity);
                } catch (DataIntegrityViolationException ex) {
                        if (isProjectCodeDuplicate(ex)) {
                                throw new DuplicateProjectCodeException(
                                                "Mã dự án '" + project.getProjectCode()
                                                                + "' đã tồn tại trong hệ thống");
                        }
                        throw ex;
                }
        }

        private boolean isProjectCodeDuplicate(DataIntegrityViolationException ex) {
                Throwable current = ex;
                while (current != null) {
                        if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                                String constraintName = cve.getConstraintName();
                                if ("uk_projects_project_code".equalsIgnoreCase(constraintName)) {
                                        return true;
                                }
                        }
                        current = current.getCause();
                }
                return false;
        }
}
