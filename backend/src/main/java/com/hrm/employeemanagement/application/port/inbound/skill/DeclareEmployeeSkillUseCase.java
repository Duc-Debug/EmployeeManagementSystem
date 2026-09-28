package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;

public interface DeclareEmployeeSkillUseCase {

    EmployeeSkillResult execute(DeclareEmployeeSkillCommand command);
}
