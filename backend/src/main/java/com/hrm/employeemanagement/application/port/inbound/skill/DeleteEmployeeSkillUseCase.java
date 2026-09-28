package com.hrm.employeemanagement.application.port.inbound.skill;

import com.hrm.employeemanagement.application.dto.skill.DeleteEmployeeSkillCommand;

public interface DeleteEmployeeSkillUseCase {
    void execute(DeleteEmployeeSkillCommand command);
}
