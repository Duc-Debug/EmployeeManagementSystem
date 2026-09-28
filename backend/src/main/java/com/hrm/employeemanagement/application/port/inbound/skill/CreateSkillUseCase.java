package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.CreateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;

public interface CreateSkillUseCase {
    SkillResult execute(CreateSkillCommand command);
}