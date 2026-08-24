package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SpringDataOrgUnitRepository extends JpaRepository<OrgUnitJpaEntity, Long> {
    Optional<OrgUnitJpaEntity> findByUnitCode(String unitCode);

    boolean existsByUnitCode(String unitCode);

    @Query(value = """
            SELECT CASE
                       WHEN COUNT(*) > 0 THEN TRUE
                       ELSE FALSE
                   END
            FROM org_units ou
            JOIN org_units scope
                ON scope.id = :scopeOrgUnitId
            WHERE ou.id = :orgUnitId
              AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
            """,
            nativeQuery = true)
    boolean existsInOrgUnitBranch(
            @Param("orgUnitId") Long orgUnitId,
            @Param("scopeOrgUnitId") Long scopeOrgUnitId
    );

    List<OrgUnitJpaEntity> findByStatus(OrgUnitStatus status);

    List<OrgUnitJpaEntity> findByTreePathStartingWith(String treePath);
}
