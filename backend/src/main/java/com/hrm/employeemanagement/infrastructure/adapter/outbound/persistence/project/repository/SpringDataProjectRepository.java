package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;

@Repository
public interface SpringDataProjectRepository
        extends JpaRepository<ProjectJpaEntity, Long> {

    @Query(value = """
        SELECT p.*
        FROM projects p
        ORDER BY p.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<ProjectJpaEntity> findAllOrdered(
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT p.*
        FROM projects p
        JOIN org_units ou
            ON ou.id = p.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        ORDER BY p.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<ProjectJpaEntity> findByOrgUnitBranch(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM projects p
        JOIN org_units ou
            ON ou.id = p.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        """,
        nativeQuery = true)
    long countByOrgUnitBranch(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId
    );

    @Query(value = """
        SELECT p.*
        FROM projects p
        WHERE p.manager_id = :employeeId
        ORDER BY p.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<ProjectJpaEntity> findManagedBy(
            @Param("employeeId") Long employeeId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM projects p
        WHERE p.manager_id = :employeeId
        """,
        nativeQuery = true)
    long countManagedBy(
            @Param("employeeId") Long employeeId
    );

    @Query(value = """
        SELECT DISTINCT p.*
        FROM projects p
        JOIN project_members pm
            ON pm.project_id = p.id
        WHERE pm.employee_id = :employeeId
        ORDER BY p.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<ProjectJpaEntity> findMemberProjects(
            @Param("employeeId") Long employeeId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM projects p
        JOIN project_members pm
            ON pm.project_id = p.id
        WHERE pm.employee_id = :employeeId
        """,
        nativeQuery = true)
    long countMemberProjects(
            @Param("employeeId") Long employeeId
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM projects p
        JOIN org_units ou
            ON ou.id = p.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE p.id = :projectId
          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        """,
        nativeQuery = true)
    int countInOrgUnitBranch(
            @Param("projectId") Long projectId,
            @Param("scopeOrgUnitId") Long scopeOrgUnitId
    );

    default boolean existsInOrgUnitBranch(Long projectId, Long scopeOrgUnitId) {
        return countInOrgUnitBranch(projectId, scopeOrgUnitId) > 0;
    }

    @Query(value = """
        SELECT COUNT(*)
        FROM projects p
        WHERE p.id = :projectId
          AND p.manager_id = :employeeId
        """,
        nativeQuery = true)
    int countManagedBy(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId
    );

    default boolean existsManagedBy(Long projectId, Long employeeId) {
        return countManagedBy(projectId, employeeId) > 0;
    }

    @Query(value = """
        SELECT COUNT(*)
        FROM project_members pm
        WHERE pm.project_id = :projectId
          AND pm.employee_id = :employeeId
        """,
        nativeQuery = true)
    int countMember(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId
    );

    default boolean existsMember(Long projectId, Long employeeId) {
        return countMember(projectId, employeeId) > 0;
    }
}
