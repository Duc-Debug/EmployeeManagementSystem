package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateEmployeeSkillCommand;

public interface UpdateEmployeeSkillUseCase {
    EmployeeSkillResult execute(UpdateEmployeeSkillCommand command);
}
