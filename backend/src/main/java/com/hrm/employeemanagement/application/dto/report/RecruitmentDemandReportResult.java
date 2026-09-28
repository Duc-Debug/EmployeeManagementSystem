package com.hrm.employeemanagement.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.hrm.employeemanagement.domain.report.RecruitmentSkillDemand;

public record RecruitmentDemandReportResult(
        int totalSkillsEvaluated,
        BigDecimal totalDeficitHours,
        int skillsWithDeficitCount,
        List<RecruitmentSkillDemand> skills,
        String timeRangeText,
        LocalDateTime generatedAt,
        BigDecimal unmappedDemandHours,
        int unmappedRoleCount,
        BigDecimal unattributedCapacityHours
) {
    public RecruitmentDemandReportResult(
            int totalSkillsEvaluated,
            BigDecimal totalDeficitHours,
            int skillsWithDeficitCount,
            List<RecruitmentSkillDemand> skills,
            String timeRangeText,
            LocalDateTime generatedAt
    ) {
        this(totalSkillsEvaluated, totalDeficitHours, skillsWithDeficitCount, skills, timeRangeText, generatedAt,
                BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }
}
