package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Thông tin nhân sự thỏa mãn điều kiện kỹ năng kèm mức độ rảnh.
 */
public record ResourceSearchResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long orgUnitId,
        String orgUnitName,
        String jobTitle,
        Long skillId,
        String skillName,
        Integer proficiencyLevel,
        BigDecimal yearsOfExperience,
        List<WeeklyAvailableHoursResult> weeklyAvailabilities,
        BigDecimal totalRemainingHours, // Tổng số giờ rảnh tích lũy trong toàn bộ khoảng tuần (dùng để sắp xếp)
        Boolean isOutsourced,
        String providerName,
        LocalDate contractStartDate,
        LocalDate contractEndDate
) {
    public ResourceSearchResult(
            Long employeeId,
            String employeeCode,
            String fullName,
            Long orgUnitId,
            String orgUnitName,
            String jobTitle,
            Long skillId,
            String skillName,
            Integer proficiencyLevel,
            BigDecimal yearsOfExperience,
            List<WeeklyAvailableHoursResult> weeklyAvailabilities,
            BigDecimal totalRemainingHours
    ) {
        this(
                employeeId,
                employeeCode,
                fullName,
                orgUnitId,
                orgUnitName,
                jobTitle,
                skillId,
                skillName,
                proficiencyLevel,
                yearsOfExperience,
                weeklyAvailabilities,
                totalRemainingHours,
                false,
                null,
                null,
                null
        );
    }
}
