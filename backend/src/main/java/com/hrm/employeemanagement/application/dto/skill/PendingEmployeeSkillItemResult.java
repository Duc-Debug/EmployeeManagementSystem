package com.hrm.employeemanagement.application.dto.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PendingEmployeeSkillItemResult(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        Long orgUnitId,
        String orgUnitName,
        Long skillId,
        String skillCode,
        String skillName,
        String skillCategory,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience,
        String status,
        LocalDateTime createdAt
) {}
