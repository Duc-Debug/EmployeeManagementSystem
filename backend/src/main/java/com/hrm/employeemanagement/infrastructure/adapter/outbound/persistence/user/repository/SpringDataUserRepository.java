package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.role.code = 'VT-06' AND u.isActive = true")
    long countActiveAdmins();

    @Query(value = """
        SELECT DISTINCT u.*
        FROM users u
        JOIN employees e
            ON e.user_id = u.id
        JOIN org_units ou
            ON ou.id = e.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        ORDER BY u.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
     List<UserJpaEntity> findByOrgUnitBranch(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT u.id)
        FROM users u
        JOIN employees e
            ON e.user_id = u.id
        JOIN org_units ou
            ON ou.id = e.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        """,
        nativeQuery = true)
    long countByOrgUnitBranch(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId
    );
}