package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillCommand;

public interface UpdateSkillUseCase {
    SkillResult execute(UpdateSkillCommand command);
}
