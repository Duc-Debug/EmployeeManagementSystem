package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import java.util.List;

public interface SpringDataEmployeeSkillRepository extends JpaRepository<EmployeeSkillJpaEntity, EmployeeSkillId> {

    @Query("SELECT es.employeeId FROM EmployeeSkillJpaEntity es WHERE es.skillId = :skillId")
    List<Long> findEmployeeIdsBySkillId(@Param("skillId") Long skillId);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    long countBySkillId(Long skillId);

    @Modifying
    @Query("""
        DELETE FROM EmployeeSkillJpaEntity es
        WHERE es.skillId = :sourceSkillId
          AND es.employeeId IN (
              SELECT es2.employeeId FROM EmployeeSkillJpaEntity es2 WHERE es2.skillId = :targetSkillId
          )
    """)
    int deleteDuplicateEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    @Modifying
    @Query("UPDATE EmployeeSkillJpaEntity es SET es.skillId = :targetSkillId WHERE es.skillId = :sourceSkillId")
    int reassignEmployeeSkills(@Param("sourceSkillId") Long sourceSkillId, @Param("targetSkillId") Long targetSkillId);

    @Modifying
    @Query("DELETE FROM EmployeeSkillJpaEntity es WHERE es.employeeId = :employeeId AND es.skillId = :skillId")
    void deleteByEmployeeIdAndSkillId(@Param("employeeId") Long employeeId, @Param("skillId") Long skillId);
}
