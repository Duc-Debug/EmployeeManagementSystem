package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;

import com.hrm.employeemanagement.application.dto.skill.RejectEmployeeSkillCommand;

public interface ApproveEmployeeSkillUseCase {

    EmployeeSkillResult execute(ApproveEmployeeSkillCommand command);

    EmployeeSkillResult reject(RejectEmployeeSkillCommand command);
}
