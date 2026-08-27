package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.skill.SkillGroup;
import com.hrm.employeemanagement.domain.skill.SkillGroupId;

public interface LoadSkillGroupPort {
    Optional<SkillGroup> findById(SkillGroupId id);
    List<SkillGroup> findAllByIdIn(List<Long> ids);
    List<SkillGroup> findAll();
    boolean existsById(SkillGroupId id);
    boolean existsGroupByNameIgnoreCase(String name);
    boolean existsGroupByNameIgnoreCaseAndIdNot(String name, Long id);
}