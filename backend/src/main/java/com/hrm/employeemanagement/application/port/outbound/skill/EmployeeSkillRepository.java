package com.hrm.employeemanagement.application.port.outbound.skill;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;

public interface EmployeeSkillRepository {

    EmployeeSkill save(EmployeeSkill employeeSkill);

    Optional<EmployeeSkill> findById(Long id);

    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    boolean existsByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    List<EmployeeSkill> findByEmployeeId(Long employeeId);

    List<EmployeeSkill> findByStatus(com.hrm.employeemanagement.domain.skill.SkillStatus status);

    List<EmployeeSkill> findByStatusAndEmployeeIdIn(com.hrm.employeemanagement.domain.skill.SkillStatus status, List<Long> employeeIds);

    PageResult<PendingEmployeeSkillItemResult> findPendingSkills(
            DataScope dataScope,
            Long scopeOrgUnitId,
            Long currentUserId,
            String keyword,
            int page,
            int size
    );
}

