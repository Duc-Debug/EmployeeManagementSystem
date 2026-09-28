package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.id = :id")
    Optional<EmployeeJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<EmployeeJpaEntity> findByUserId(Long userId);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);
    List<EmployeeJpaEntity> findByUserIdIn(List<Long> userIds);
    List<EmployeeJpaEntity> findByOrgUnitId(Long orgUnitId);
    List<EmployeeJpaEntity> findByOrgUnitIdAndStatus(Long orgUnitId, String status);
    List<EmployeeJpaEntity> findByOrgUnitIdInAndStatus(List<Long> orgUnitIds, String status);

    @Query("SELECT e FROM EmployeeJpaEntity e WHERE UPPER(e.status) = UPPER(:status) OR (e.status IS NULL AND UPPER(:status) = 'ACTIVE')")
    List<EmployeeJpaEntity> findByStatus(@Param("status") String status);

    @Query(value = """
        SELECT e.*
        FROM employees e
        ORDER BY e.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<EmployeeJpaEntity> findAllPaged(
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT DISTINCT e.*
        FROM employees e
        JOIN org_units ou
            ON ou.id = e.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        ORDER BY e.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<EmployeeJpaEntity> findByOrgUnitBranch(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT e.id)
        FROM employees e
        JOIN org_units ou
            ON ou.id = e.org_unit_id
        JOIN org_units scope
            ON scope.id = :scopeOrgUnitId
        WHERE ou.tree_path LIKE CONCAT(scope.tree_path, '%')
        """,
        nativeQuery = true)
    long countByOrgUnitBranch(@Param("scopeOrgUnitId") Long scopeOrgUnitId);

    @Query(value = """
        SELECT DISTINCT e.*
        FROM employees e
        WHERE e.id = :pmEmployeeId
           OR e.id IN (
               SELECT pm.employee_id
               FROM project_members pm
               JOIN projects p ON p.id = pm.project_id
               WHERE p.manager_id = :pmEmployeeId
           )
        ORDER BY e.id DESC
        LIMIT :size OFFSET :offset
        """,
        nativeQuery = true)
    List<EmployeeJpaEntity> findByProjectManager(
            @Param("pmEmployeeId") Long pmEmployeeId,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT e.id)
        FROM employees e
        WHERE e.id = :pmEmployeeId
           OR e.id IN (
               SELECT pm.employee_id
               FROM project_members pm
               JOIN projects p ON p.id = pm.project_id
               WHERE p.manager_id = :pmEmployeeId
           )
        """,
        nativeQuery = true)
    long countByProjectManager(@Param("pmEmployeeId") Long pmEmployeeId);

    @Query(value = """
        SELECT e.*
        FROM employees e
        WHERE e.status = 'ACTIVE'
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.professional_role) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY e.full_name ASC, e.employee_code ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<EmployeeJpaEntity> findActivePaged(
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM employees e
        WHERE e.status = 'ACTIVE'
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.professional_role) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        """, nativeQuery = true)
    long countActivePaged(@Param("keyword") String keyword);

    @Query(value = """
        SELECT e.*
        FROM employees e
        WHERE e.status = 'ACTIVE'
          AND e.org_unit_id IN (:orgUnitIds)
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.professional_role) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY e.full_name ASC, e.employee_code ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<EmployeeJpaEntity> findActiveByOrgUnitIdsPaged(
            @Param("orgUnitIds") List<Long> orgUnitIds,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM employees e
        WHERE e.status = 'ACTIVE'
          AND e.org_unit_id IN (:orgUnitIds)
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.professional_role) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        """, nativeQuery = true)
    long countActiveByOrgUnitIdsPaged(
            @Param("orgUnitIds") List<Long> orgUnitIds,
            @Param("keyword") String keyword
    );
}
