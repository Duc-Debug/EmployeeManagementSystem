package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;
import com.hrm.employeemanagement.domain.skill.SkillStatus;

public interface LoadSkillPort {
    Optional<Skill> findById(SkillId id);
    List<Skill> findAllByIdIn(List<Long> ids);
    List<Skill> findAll(Long groupId, SkillStatus status, String keyword);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<Long> findEmployeeIdsWithSkill(Long skillId);
    boolean hasEmployeeSkill(Long employeeId, Long skillId);
    long countByGroupIdAndStatus(Long groupId, SkillStatus status);
}