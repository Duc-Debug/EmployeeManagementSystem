package com.hrm.employeemanagement.application.port.inbound.skill;

import java.util.List;

import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;

public interface GetPendingEmployeeSkillsUseCase {

    List<PendingEmployeeSkillItemResult> execute(String keyword);

    PageResult<PendingEmployeeSkillItemResult> execute(String keyword, int page, int size);
}
