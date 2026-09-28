package com.hrm.employeemanagement.application.dto.skill;

public record RejectEmployeeSkillCommand(
        Long employeeSkillId,
        String rejectionReason
) {}
