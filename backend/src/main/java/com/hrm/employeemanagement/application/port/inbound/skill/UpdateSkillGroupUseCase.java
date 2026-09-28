package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillGroupCommand;

public interface UpdateSkillGroupUseCase {
    SkillGroupResult execute(UpdateSkillGroupCommand command);
}
