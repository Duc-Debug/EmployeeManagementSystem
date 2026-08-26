package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit;

import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrgUnitRepositoryAdapter implements LoadOrgUnitPort, SaveOrgUnitPort {
    private final SpringDataOrgUnitRepository repository;

    public OrgUnitRepositoryAdapter(SpringDataOrgUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OrgUnit> findById(OrgUnitId id) {
        if (id == null || id.getValue() == null)
            return Optional.empty();
        return repository.findById(id.getValue())
                .map(OrgUnitPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OrgUnit> findByUnitCode(String unitCode) {
        return repository.findByUnitCode(unitCode)
                .map(OrgUnitPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUnitCode(String unitCode) {
        return repository.existsByUnitCode(unitCode);
    }

    @Override
    public boolean existsInOrgUnitBranch(
            Long orgUnitId,
            Long scopeOrgUnitId
    ) {
        return repository.existsInOrgUnitBranch(
                orgUnitId,
                scopeOrgUnitId
        );
    }

    @Override
    public List<OrgUnit> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream()
                .map(OrgUnitPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findAllActive() {
        return repository.findByStatus(OrgUnitStatus.ACTIVE).stream()
                .map(OrgUnitPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findAll() {
        return repository.findAll().stream()
                .map(OrgUnitPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findSubTree(String treePath) {
        return repository.findByTreePathStartingWith(treePath).stream()
                .map(OrgUnitPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OrgUnit save(OrgUnit orgUnit) {
        try {
            OrgUnitJpaEntity jpaEntity = OrgUnitPersistenceMapper.toJpaEntity(orgUnit);
            OrgUnitJpaEntity savedEntity = repository.save(jpaEntity);
            if (orgUnit.getId() == null) {
                String finalTreePath = savedEntity.getTreePath() + savedEntity.getId() + "/";
                savedEntity.setTreePath(finalTreePath);
                savedEntity = repository.save(savedEntity);
            }
            return OrgUnitPersistenceMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            String lowerMsg = rootMsg != null ? rootMsg.toLowerCase() : "";
            if (lowerMsg.contains("unit_code") || lowerMsg.contains("duplicate") || lowerMsg.contains("unique") || lowerMsg.contains("uk_")) {
                throw new DuplicateUnitCodeException(
                        "Mã đơn vị '" + orgUnit.getUnitCode() + "' đã tồn tại trong hệ thống");
            }
            throw ex;
        }
    }

    @Override
    public int updateSubTreePaths(String oldPrefix, String newPrefix, int levelDelta) {
        return repository.updateSubTreePaths(oldPrefix, newPrefix, levelDelta);
    }

    @Override
    public int deactivateSubTree(String treePath) {
        return repository.deactivateSubTree(treePath);
    }
}
