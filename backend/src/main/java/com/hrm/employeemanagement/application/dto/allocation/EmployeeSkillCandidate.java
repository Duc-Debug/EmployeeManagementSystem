package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;

import com.hrm.employeemanagement.domain.employee.Employee;

/**
 * Bản ghi nhân sự kèm thông tin kỹ năng đã được duyệt từ tầng lưu trữ.
 *
 * @deprecated Thay thế bằng {@link ResourceCandidate} để tuân thủ kiến trúc phân tách ranh giới Domain/Port.
 */
@Deprecated(forRemoval = true, since = "1.0")
public record EmployeeSkillCandidate(
        Employee employee,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience
) {

}
