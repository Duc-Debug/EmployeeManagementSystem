package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projection thông tin nhân sự kèm kỹ năng từ tầng lưu trữ phục vụ tìm kiếm nguồn lực.
 */
public record ResourceCandidate(
        Long employeeId,
        Long userId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String professionalRole,
        Integer standardHoursPerWeek,
        LocalDate contractEndDate,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience,
        Boolean isOutsourced,
        String providerName,
        LocalDate startDate
) {
    public ResourceCandidate(
            Long employeeId,
            Long userId,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String professionalRole,
            Integer standardHoursPerWeek,
            LocalDate contractEndDate,
            Long skillId,
            String skillName,
            Integer proficiencyLevel,
            BigDecimal yearsOfExperience
    ) {
        this(
                employeeId,
                userId,
                employeeCode,
                fullName,
                orgUnitId,
                professionalRole,
                standardHoursPerWeek,
                contractEndDate,
                skillId,
                skillName,
                proficiencyLevel,
                yearsOfExperience,
                false,
                null,
                null
        );
    }
}
