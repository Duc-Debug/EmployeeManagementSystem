package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.MergeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;

public interface MergeSkillUseCase {
    SkillResult execute(MergeSkillCommand command);
}
