package com.hrm.employeemanagement.application.port.inbound.skill;

import java.util.List;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;

public interface GetSkillGroupListUseCase {
    List<SkillGroupResult> execute();
}
