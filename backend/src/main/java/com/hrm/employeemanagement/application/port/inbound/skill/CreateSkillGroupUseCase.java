package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.CreateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;

public interface CreateSkillGroupUseCase {
    SkillGroupResult execute(CreateSkillGroupCommand command);
}
