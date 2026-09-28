package com.hrm.employeemanagement.application.port.outbound.skill;

import com.hrm.employeemanagement.domain.skill.SkillGroup;

public interface SaveSkillGroupPort {
    SkillGroup save(SkillGroup skillGroup);
}
