package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SpringDataOrgUnitRepository extends JpaRepository<OrgUnitJpaEntity, Long> {
    Optional<OrgUnitJpaEntity> findByUnitCode(String unitCode);

    boolean existsByUnitCode(String unitCode);

    List<OrgUnitJpaEntity> findByStatus(OrgUnitStatus status);

    List<OrgUnitJpaEntity> findByTreePathStartingWith(String treePath);
}