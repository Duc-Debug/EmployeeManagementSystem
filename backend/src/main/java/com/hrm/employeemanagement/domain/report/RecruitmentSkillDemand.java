package com.hrm.employeemanagement.domain.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class RecruitmentSkillDemand {

    private final Long skillId;
    private final String skillCode;
    private final String skillName;
    private final String category;
    private final BigDecimal requiredDemandHours;
    private final BigDecimal availableCapacityHours;
    private final BigDecimal shortfallHours;
    private final String status; // "DEFICIT" hoặc "SUFFICIENT"

    public RecruitmentSkillDemand(
            Long skillId,
            String skillCode,
            String skillName,
            String category,
            BigDecimal requiredDemandHours,
            BigDecimal availableCapacityHours
    ) {
        this.skillId = skillId;
        this.skillCode = Objects.requireNonNull(skillCode, "skillCode must not be null");
        this.skillName = Objects.requireNonNull(skillName, "skillName must not be null");
        this.category = category != null ? category : "Chung";
        this.requiredDemandHours = requiredDemandHours != null ? requiredDemandHours.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.availableCapacityHours = availableCapacityHours != null ? availableCapacityHours.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);

        BigDecimal diff = this.requiredDemandHours.subtract(this.availableCapacityHours);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            this.shortfallHours = diff.setScale(2, RoundingMode.HALF_UP);
            this.status = "DEFICIT";
        } else {
            this.shortfallHours = BigDecimal.ZERO.setScale(2);
            this.status = "SUFFICIENT";
        }
    }

    public Long getSkillId() {
        return skillId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getRequiredDemandHours() {
        return requiredDemandHours;
    }

    public BigDecimal getAvailableCapacityHours() {
        return availableCapacityHours;
    }

    public BigDecimal getShortfallHours() {
        return shortfallHours;
    }

    public String getStatus() {
        return status;
    }

    public boolean isDeficit() {
        return "DEFICIT".equals(status);
    }
}
