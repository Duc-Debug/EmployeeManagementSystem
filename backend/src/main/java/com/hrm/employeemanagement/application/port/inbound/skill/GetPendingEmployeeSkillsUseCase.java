package com.hrm.employeemanagement.application.port.inbound.skill;

import java.util.List;

import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;

public interface GetPendingEmployeeSkillsUseCase {

    List<PendingEmployeeSkillItemResult> execute(String keyword);
}
