package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "scenario_allocation_snapshot",
       uniqueConstraints = @UniqueConstraint(name = "uk_sas_scenario_emp_week", columnNames = {"scenario_id", "employee_id", "year_number", "week_number"}))
public class ScenarioAllocationSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "allocated_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocatedHours;

    @Column(name = "available_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal availableHours;

    public ScenarioAllocationSnapshotJpaEntity() {}

    public ScenarioAllocationSnapshotJpaEntity(
            Long id,
            Long scenarioId,
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            BigDecimal allocatedHours,
            BigDecimal availableHours
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.employeeId = employeeId;
        this.yearNumber = yearNumber;
        this.weekNumber = weekNumber;
        this.allocatedHours = allocatedHours;
        this.availableHours = availableHours;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getYearNumber() { return yearNumber; }
    public void setYearNumber(Integer yearNumber) { this.yearNumber = yearNumber; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public BigDecimal getAllocatedHours() { return allocatedHours; }
    public void setAllocatedHours(BigDecimal allocatedHours) { this.allocatedHours = allocatedHours; }
    public BigDecimal getAvailableHours() { return availableHours; }
    public void setAvailableHours(BigDecimal availableHours) { this.availableHours = availableHours; }
}
