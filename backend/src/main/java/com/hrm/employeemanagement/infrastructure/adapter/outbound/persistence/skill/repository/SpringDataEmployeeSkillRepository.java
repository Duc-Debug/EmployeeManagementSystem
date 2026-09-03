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

    @Query("SELECT es.employeeId FROM EmployeeSkillJpaEntity es WHERE es.skillId = :skillId")
    List<Long> findEmployeeIdsBySkillId(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "DELETE es FROM employee_skills es WHERE es.skill_id = :sourceSkillId AND EXISTS (SELECT 1 FROM (SELECT employee_id FROM employee_skills WHERE skill_id = :targetSkillId) t WHERE t.employee_id = es.employee_id)", nativeQuery = true)
    int deleteDuplicateEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    @Modifying
    @Query("UPDATE EmployeeSkillJpaEntity es SET es.skillId = :targetSkillId WHERE es.skillId = :sourceSkillId")
    int reassignEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    void deleteByEmployeeIdAndSkillId(Long employeeId, Long skillId);
}
