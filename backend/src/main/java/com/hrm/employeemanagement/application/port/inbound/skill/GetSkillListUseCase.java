package com.hrm.employeemanagement.application.port.inbound.skill;

import java.util.List;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;

public interface GetSkillListUseCase {
    List<SkillResult> execute(Long groupId, String status, String keyword);
}
