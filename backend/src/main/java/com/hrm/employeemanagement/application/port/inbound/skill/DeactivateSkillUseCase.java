package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;

public interface DeactivateSkillUseCase {
    SkillResult execute(DeactivateSkillCommand command);
}
