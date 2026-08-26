package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;

public interface LoadSkillPort {
    Optional<Skill> findById(SkillId id);
    List<Skill> findAllByIdIn(List<Long> ids);
    List<Skill> findAll(Long groupId, String status, String keyword);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<Long> findEmployeeIdsWithSkill(Long skillId);
    boolean hasEmployeeSkill(Long employeeId, Long skillId);
    long countByGroupIdAndStatus(Long groupId, com.hrm.employeemanagement.domain.skill.SkillStatus status);
}