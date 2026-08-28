package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;

public interface SpringDataEmployeeSkillRepository extends JpaRepository<EmployeeSkillJpaEntity, Long> {

    Optional<EmployeeSkillJpaEntity> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    List<EmployeeSkillJpaEntity> findByEmployeeId(Long employeeId);
}
