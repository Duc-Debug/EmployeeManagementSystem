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
            SELECT COUNT(*)
            FROM org_units ou
            JOIN org_units scope
                ON scope.id = :scopeOrgUnitId
            WHERE ou.id = :orgUnitId
              AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
            """,
            nativeQuery = true)
    int countInOrgUnitBranch(
            @Param("orgUnitId") Long orgUnitId,
            @Param("scopeOrgUnitId") Long scopeOrgUnitId
    );

    default boolean existsInOrgUnitBranch(Long orgUnitId, Long scopeOrgUnitId) {
        return countInOrgUnitBranch(orgUnitId, scopeOrgUnitId) > 0;
    }

    List<OrgUnitJpaEntity> findByStatus(OrgUnitStatus status);

    List<OrgUnitJpaEntity> findAllByOrderByTreePathAscUnitCodeAsc();

    List<OrgUnitJpaEntity> findByTreePathStartingWith(String treePath);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE org_units
            SET tree_path = CONCAT(:newPrefix, SUBSTRING(tree_path, LENGTH(:oldPrefix) + 1)),
                level = level + :levelDelta,
                updated_at = CURRENT_TIMESTAMP
            WHERE tree_path LIKE CONCAT(:oldPrefix, '%')
            """,
            nativeQuery = true)
    int updateSubTreePaths(
            @Param("oldPrefix") String oldPrefix,
            @Param("newPrefix") String newPrefix,
            @Param("levelDelta") int levelDelta
    );

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE org_units
            SET status = 'INACTIVE',
                updated_at = CURRENT_TIMESTAMP
            WHERE tree_path LIKE CONCAT(:treePath, '%')
              AND status = 'ACTIVE'
            """,
            nativeQuery = true)
    int deactivateSubTree(@Param("treePath") String treePath);
}
