package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;

public interface ApproveEmployeeSkillUseCase {

    EmployeeSkillResult execute(ApproveEmployeeSkillCommand command);
}
