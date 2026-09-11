package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPermissionRepository
        extends JpaRepository<PermissionJpaEntity, Long> {

    @Query(value = """
            SELECT COUNT(*)
            FROM users u
            JOIN role_permissions rp
                ON rp.role_id = u.role_id
            JOIN permissions p
                ON p.id = rp.permission_id
            WHERE u.id = :userId
              AND u.is_active = TRUE
              AND p.code = :permissionCode
            """, nativeQuery = true)
    long countPermissionMatches(
            @Param("userId") Long userId,
            @Param("permissionCode") String permissionCode
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.id = :userId
              AND u.is_active = TRUE
              AND r.code = :roleCode
            """, nativeQuery = true)
    long countUserRoleMatches(
            @Param("userId") Long userId,
            @Param("roleCode") String roleCode
    );

    @Query(value = """
            SELECT p.code
            FROM users u
            JOIN role_permissions rp
                ON rp.role_id = u.role_id
            JOIN permissions p
                ON p.id = rp.permission_id
            WHERE u.id = :userId
              AND u.is_active = TRUE
            """, nativeQuery = true)
    java.util.List<String> findPermissionCodesByUserId(
            @Param("userId") Long userId
    );
}