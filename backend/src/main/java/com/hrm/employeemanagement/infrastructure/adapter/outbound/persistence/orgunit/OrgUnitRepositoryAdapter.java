package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit;

import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
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
        if (id == null || id.getValue() == null) return Optional.empty();
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
    public List<OrgUnit> findAllActive() {
        return repository.findByStatus(OrgUnitStatus.ACTIVE).stream()
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
        OrgUnitJpaEntity jpaEntity = OrgUnitPersistenceMapper.toJpaEntity(orgUnit);
        OrgUnitJpaEntity savedEntity = repository.save(jpaEntity);
        return OrgUnitPersistenceMapper.toDomain(savedEntity);
    }
}