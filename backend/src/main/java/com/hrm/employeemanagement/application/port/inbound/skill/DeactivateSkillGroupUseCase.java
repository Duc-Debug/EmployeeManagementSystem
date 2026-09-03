package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;

public interface DeactivateSkillGroupUseCase {
    SkillGroupResult execute(DeactivateSkillGroupCommand command);
}
