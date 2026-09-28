package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.util.Objects;

public class ScenarioAllocationSnapshotItem {
    private Long id;
    private Long scenarioId;
    private Long employeeId;
    private Integer yearNumber;
    private Integer weekNumber;
    private BigDecimal allocatedHours;
    private BigDecimal availableHours;

    public ScenarioAllocationSnapshotItem(
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
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.yearNumber = Objects.requireNonNull(yearNumber, "yearNumber must not be null");
        this.weekNumber = Objects.requireNonNull(weekNumber, "weekNumber must not be null");
        this.allocatedHours = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        this.availableHours = availableHours != null ? availableHours : BigDecimal.ZERO;
    }

    public static ScenarioAllocationSnapshotItem create(
            Long scenarioId,
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            BigDecimal allocatedHours,
            BigDecimal availableHours
    ) {
        return new ScenarioAllocationSnapshotItem(
                null,
                scenarioId,
                employeeId,
                yearNumber,
                weekNumber,
                allocatedHours,
                availableHours
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getEmployeeId() { return employeeId; }
    public Integer getYearNumber() { return yearNumber; }
    public Integer getWeekNumber() { return weekNumber; }
    public BigDecimal getAllocatedHours() { return allocatedHours; }
    public BigDecimal getAvailableHours() { return availableHours; }
}
