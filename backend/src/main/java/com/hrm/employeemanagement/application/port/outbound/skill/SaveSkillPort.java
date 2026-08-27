package com.hrm.employeemanagement.application.port.outbound.skill;

import com.hrm.employeemanagement.domain.skill.Skill;

public interface SaveSkillPort {
    Skill save(Skill skill);
    int deleteDuplicateEmployeeSkills(Long sourceSkillId, Long targetSkillId);
    int reassignEmployeeSkills(Long sourceSkillId, Long targetSkillId);
    void removeEmployeeSkill(Long employeeId, Long skillId);
}

