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
}