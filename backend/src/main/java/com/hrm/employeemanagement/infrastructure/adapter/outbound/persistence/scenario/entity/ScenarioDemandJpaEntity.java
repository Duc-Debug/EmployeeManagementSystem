package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "scenario_demands")
public class ScenarioDemandJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "demand_name", nullable = false, length = 255)
    private String demandName;

    @Column(name = "headcount", nullable = false)
    private Integer headcount;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "start_week", nullable = false)
    private Integer startWeek;

    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    @Column(name = "end_week", nullable = false)
    private Integer endWeek;

    @Column(name = "hours_per_week_per_person", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursPerWeekPerPerson;

    @Column(name = "skill_requirement", length = 255)
    private String skillRequirement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ScenarioDemandJpaEntity() {}

    public ScenarioDemandJpaEntity(
            Long id,
            Long scenarioId,
            String demandName,
            Integer headcount,
            Integer startYear,
            Integer startWeek,
            Integer endYear,
            Integer endWeek,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.demandName = demandName;
        this.headcount = headcount;
        this.startYear = startYear;
        this.startWeek = startWeek;
        this.endYear = endYear;
        this.endWeek = endWeek;
        this.hoursPerWeekPerPerson = hoursPerWeekPerPerson;
        this.skillRequirement = skillRequirement;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getDemandName() { return demandName; }
    public void setDemandName(String demandName) { this.demandName = demandName; }
    public Integer getHeadcount() { return headcount; }
    public void setHeadcount(Integer headcount) { this.headcount = headcount; }
    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }
    public Integer getStartWeek() { return startWeek; }
    public void setStartWeek(Integer startWeek) { this.startWeek = startWeek; }
    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }
    public Integer getEndWeek() { return endWeek; }
    public void setEndWeek(Integer endWeek) { this.endWeek = endWeek; }
    public BigDecimal getHoursPerWeekPerPerson() { return hoursPerWeekPerPerson; }
    public void setHoursPerWeekPerPerson(BigDecimal hoursPerWeekPerPerson) { this.hoursPerWeekPerPerson = hoursPerWeekPerPerson; }
    public String getSkillRequirement() { return skillRequirement; }
    public void setSkillRequirement(String skillRequirement) { this.skillRequirement = skillRequirement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
