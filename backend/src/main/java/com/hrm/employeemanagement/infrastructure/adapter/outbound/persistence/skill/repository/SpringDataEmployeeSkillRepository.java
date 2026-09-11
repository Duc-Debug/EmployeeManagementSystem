package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;

public interface SpringDataEmployeeSkillRepository extends JpaRepository<EmployeeSkillJpaEntity, Long> {

    Optional<EmployeeSkillJpaEntity> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    List<EmployeeSkillJpaEntity> findByEmployeeId(Long employeeId);

    List<EmployeeSkillJpaEntity> findByStatus(com.hrm.employeemanagement.domain.skill.SkillStatus status);

    List<EmployeeSkillJpaEntity> findByStatusAndEmployeeIdIn(com.hrm.employeemanagement.domain.skill.SkillStatus status, List<Long> employeeIds);

    @Query("SELECT es.employeeId FROM EmployeeSkillJpaEntity es WHERE es.skillId = :skillId")
    List<Long> findEmployeeIdsBySkillId(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "DELETE es FROM employee_skills es WHERE es.skill_id = :sourceSkillId AND EXISTS (SELECT 1 FROM (SELECT employee_id FROM employee_skills WHERE skill_id = :targetSkillId) t WHERE t.employee_id = es.employee_id)", nativeQuery = true)
    int deleteDuplicateEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    @Modifying
    @Query("UPDATE EmployeeSkillJpaEntity es SET es.skillId = :targetSkillId WHERE es.skillId = :sourceSkillId")
    int reassignEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM EmployeeSkillJpaEntity es WHERE es.employeeId = :employeeId AND es.skillId = :skillId")
    void deleteByEmployeeIdAndSkillId(@Param("employeeId") Long employeeId, @Param("skillId") Long skillId);

    @Query(value = """
        SELECT 
            es.id AS id,
            e.id AS employeeId,
            e.employee_code AS employeeCode,
            e.full_name AS employeeName,
            e.org_unit_id AS orgUnitId,
            ou.unit_name AS orgUnitName,
            s.id AS skillId,
            s.code AS skillCode,
            s.name AS skillName,
            s.category AS skillCategory,
            es.proficiency_level AS proficiencyLevel,
            es.years_of_experience AS yearsOfExperience,
            es.status AS status,
            es.created_at AS createdAt
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        LEFT JOIN org_units ou ON ou.id = e.org_unit_id
        WHERE es.status = 'PENDING'
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY es.created_at DESC, es.id DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.PendingEmployeeSkillProjection> findPendingCompanyScope(
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        WHERE es.status = 'PENDING'
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        """, nativeQuery = true)
    long countPendingCompanyScope(@Param("keyword") String keyword);

    @Query(value = """
        SELECT 
            es.id AS id,
            e.id AS employeeId,
            e.employee_code AS employeeCode,
            e.full_name AS employeeName,
            e.org_unit_id AS orgUnitId,
            ou.unit_name AS orgUnitName,
            s.id AS skillId,
            s.code AS skillCode,
            s.name AS skillName,
            s.category AS skillCategory,
            es.proficiency_level AS proficiencyLevel,
            es.years_of_experience AS yearsOfExperience,
            es.status AS status,
            es.created_at AS createdAt
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        JOIN org_units ou ON ou.id = e.org_unit_id
        JOIN org_units scope ON scope.id = :scopeOrgUnitId
        WHERE es.status = 'PENDING'
          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY es.created_at DESC, es.id DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.PendingEmployeeSkillProjection> findPendingBranchScope(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        JOIN org_units ou ON ou.id = e.org_unit_id
        JOIN org_units scope ON scope.id = :scopeOrgUnitId
        WHERE es.status = 'PENDING'
          AND ou.tree_path LIKE CONCAT(scope.tree_path, '%')
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        """, nativeQuery = true)
    long countPendingBranchScope(
            @Param("scopeOrgUnitId") Long scopeOrgUnitId,
            @Param("keyword") String keyword
    );

    @Query(value = """
        SELECT 
            es.id AS id,
            e.id AS employeeId,
            e.employee_code AS employeeCode,
            e.full_name AS employeeName,
            e.org_unit_id AS orgUnitId,
            ou.unit_name AS orgUnitName,
            s.id AS skillId,
            s.code AS skillCode,
            s.name AS skillName,
            s.category AS skillCategory,
            es.proficiency_level AS proficiencyLevel,
            es.years_of_experience AS yearsOfExperience,
            es.status AS status,
            es.created_at AS createdAt
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        LEFT JOIN org_units ou ON ou.id = e.org_unit_id
        WHERE es.status = 'PENDING'
          AND e.user_id = :currentUserId
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY es.created_at DESC, es.id DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.PendingEmployeeSkillProjection> findPendingSelfScope(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        WHERE es.status = 'PENDING'
          AND e.user_id = :currentUserId
          AND (:keyword IS NULL OR (
              LOWER(e.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(e.employee_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        """, nativeQuery = true)
    long countPendingSelfScope(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword
    );

    @Query("""
        SELECT es FROM EmployeeSkillJpaEntity es
        WHERE es.skillId = :skillId
          AND es.status = com.hrm.employeemanagement.domain.skill.SkillStatus.APPROVED
          AND es.proficiencyLevel >= :minLevel
    """)
    List<EmployeeSkillJpaEntity> findApprovedBySkillAndMinLevel(
            @Param("skillId") Long skillId,
            @Param("minLevel") int minLevel
    );

    @Query(value = """
        SELECT 
            e.id AS employeeId,
            e.user_id AS userId,
            e.employee_code AS employeeCode,
            e.full_name AS fullName,
            e.org_unit_id AS orgUnitId,
            e.professional_role AS professionalRole,
            e.standard_hours_per_week AS standardHoursPerWeek,
            e.contract_end_date AS contractEndDate,
            s.id AS skillId,
            s.name AS skillName,
            es.proficiency_level AS proficiencyLevel,
            es.years_of_experience AS yearsOfExperience
        FROM employee_skills es
        JOIN employees e ON e.id = es.employee_id
        JOIN skills s ON s.id = es.skill_id
        WHERE es.skill_id = :skillId
          AND es.status = 'APPROVED'
          AND es.proficiency_level >= :minLevel
          AND UPPER(e.status) = 'ACTIVE'
        ORDER BY es.years_of_experience DESC, es.proficiency_level DESC
        """, nativeQuery = true)
    List<com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.ActiveEmployeeSkillProjection> findActiveEmployeesBySkillAndMinLevel(
            @Param("skillId") Long skillId,
            @Param("minLevel") int minLevel
    );
}
