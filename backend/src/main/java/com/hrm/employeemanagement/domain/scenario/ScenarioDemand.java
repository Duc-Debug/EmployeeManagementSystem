package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;

public class ScenarioDemand {
    private Long id;
    private Long scenarioId;
    private String demandName;
    private Integer headcount;
    private Integer weekStart;
    private Integer weekEnd;
    private BigDecimal hoursPerWeekPerPerson;
    private String skillRequirement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScenarioDemand(
            Long id,
            Long scenarioId,
            String demandName,
            Integer headcount,
            Integer weekStart,
            Integer weekEnd,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.demandName = demandName;
        this.headcount = headcount;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.hoursPerWeekPerPerson = hoursPerWeekPerPerson;
        this.skillRequirement = skillRequirement;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        validate();
    }

    public static ScenarioDemand create(
            Long scenarioId,
            String demandName,
            Integer headcount,
            Integer weekStart,
            Integer weekEnd,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement
    ) {
        return new ScenarioDemand(
                null,
                scenarioId,
                demandName,
                headcount,
                weekStart,
                weekEnd,
                hoursPerWeekPerPerson,
                skillRequirement,
                LocalDateTime.now(),
                null
        );
    }

    public void update(
            String demandName,
            Integer headcount,
            Integer weekStart,
            Integer weekEnd,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement
    ) {
        this.demandName = demandName;
        this.headcount = headcount;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.hoursPerWeekPerPerson = hoursPerWeekPerPerson;
        this.skillRequirement = skillRequirement;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    public void validate() {
        if (demandName == null || demandName.trim().isEmpty()) {
            throw new InvalidScenarioDemandException("Tên nhu cầu không được để trống");
        }
        if (headcount == null || headcount <= 0) {
            throw new InvalidScenarioDemandException("Số lượng nhân sự cần phải lớn hơn 0");
        }
        if (hoursPerWeekPerPerson == null || hoursPerWeekPerPerson.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidScenarioDemandException("Số giờ/tuần/người không được âm");
        }
        if (weekStart == null || weekEnd == null || weekStart < 1 || weekStart > 53 || weekEnd < 1 || weekEnd > 53) {
            throw new InvalidScenarioDemandException("Tuần bắt đầu và tuần kết thúc phải nằm trong khoảng từ 1 đến 53");
        }
        if (weekStart > weekEnd) {
            throw new InvalidScenarioDemandException("Tuần bắt đầu không được lớn hơn tuần kết thúc");
        }
    }

    public BigDecimal getTotalHoursPerWeek() {
        if (headcount == null || hoursPerWeekPerPerson == null) {
            return BigDecimal.ZERO;
        }
        return hoursPerWeekPerPerson.multiply(BigDecimal.valueOf(headcount));
    }

    public boolean isActiveInWeek(int weekNumber) {
        return weekNumber >= weekStart && weekNumber <= weekEnd;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getDemandName() { return demandName; }
    public Integer getHeadcount() { return headcount; }
    public Integer getWeekStart() { return weekStart; }
    public Integer getWeekEnd() { return weekEnd; }
    public BigDecimal getHoursPerWeekPerPerson() { return hoursPerWeekPerPerson; }
    public String getSkillRequirement() { return skillRequirement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
