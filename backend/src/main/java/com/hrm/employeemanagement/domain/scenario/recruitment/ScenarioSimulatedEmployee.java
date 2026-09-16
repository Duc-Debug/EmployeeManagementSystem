package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.scenario.recruitment.InvalidSimulatedEmployeeException;

public class ScenarioSimulatedEmployee {

    private final SimulatedEmployeeId id;
    private final Long scenarioId;
    private String candidateName;
    private Long projectRoleId;
    private Long primarySkillId;
    private BigDecimal standardHoursPerWeek;
    private int weeksCount;
    private String notes;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public ScenarioSimulatedEmployee(
            SimulatedEmployeeId id,
            Long scenarioId,
            String candidateName,
            Long projectRoleId,
            Long primarySkillId,
            BigDecimal standardHoursPerWeek,
            int weeksCount,
            String notes,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        this.candidateName = candidateName;
        this.projectRoleId = Objects.requireNonNull(projectRoleId, "projectRoleId must not be null");
        this.primarySkillId = primarySkillId;
        this.standardHoursPerWeek = standardHoursPerWeek != null ? standardHoursPerWeek : new BigDecimal("40.00");
        this.weeksCount = weeksCount;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;

        validate();
    }

    public static ScenarioSimulatedEmployee create(
            Long scenarioId,
            String candidateName,
            Long projectRoleId,
            Long primarySkillId,
            BigDecimal standardHoursPerWeek,
            Integer weeksCount,
            String notes,
            Long createdBy
    ) {
        return new ScenarioSimulatedEmployee(
                null,
                scenarioId,
                candidateName,
                projectRoleId,
                primarySkillId,
                standardHoursPerWeek,
                weeksCount != null ? weeksCount : 4,
                notes,
                createdBy,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public void validate() {
        if (candidateName == null || candidateName.trim().isEmpty()) {
            throw new InvalidSimulatedEmployeeException("Tên nhân sự giả định không được để trống");
        }
        if (projectRoleId == null) {
            throw new InvalidSimulatedEmployeeException("Vai trò dự án không được để trống");
        }
        if (standardHoursPerWeek == null || standardHoursPerWeek.compareTo(BigDecimal.ZERO) <= 0
                || standardHoursPerWeek.compareTo(new BigDecimal("80.00")) > 0) {
            throw new InvalidSimulatedEmployeeException("Giờ chuẩn mỗi tuần phải lớn hơn 0 và không vượt quá 80 giờ");
        }
        if (weeksCount <= 0 || weeksCount > 52) {
            throw new InvalidSimulatedEmployeeException("Số tuần phải lớn hơn 0 và không vượt quá 52 tuần");
        }
    }

    public BigDecimal calculateSimulatedCapacityHours() {
        return standardHoursPerWeek.multiply(BigDecimal.valueOf(weeksCount));
    }

    public SimulatedEmployeeId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public Long getPrimarySkillId() {
        return primarySkillId;
    }

    public BigDecimal getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public int getWeeksCount() {
        return weeksCount;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
