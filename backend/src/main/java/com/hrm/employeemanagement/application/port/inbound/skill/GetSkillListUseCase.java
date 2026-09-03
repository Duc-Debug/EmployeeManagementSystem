package com.hrm.employeemanagement.application.port.inbound.skill;

import java.util.List;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.domain.skill.SkillStatus;

public interface GetSkillListUseCase {
    List<SkillResult> execute(Long groupId, SkillStatus status, String keyword);
}

