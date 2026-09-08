package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.employee.Employee;

/**
 * Bản ghi nhân sự kèm thông tin kỹ năng đã được duyệt từ tầng lưu trữ.
 */
public record EmployeeSkillCandidate(
        Employee employee,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience
        ) {

}
