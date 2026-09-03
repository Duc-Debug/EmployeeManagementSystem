package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.skill.EmployeeSkill;

public interface EmployeeSkillRepository {

    EmployeeSkill save(EmployeeSkill employeeSkill);

    Optional<EmployeeSkill> findById(Long id);

    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    List<EmployeeSkill> findByEmployeeId(Long employeeId);
}
