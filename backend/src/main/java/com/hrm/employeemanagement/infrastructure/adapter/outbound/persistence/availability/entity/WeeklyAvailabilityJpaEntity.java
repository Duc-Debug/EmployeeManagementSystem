package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_weekly_availabilities")
public class WeeklyAvailabilityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year_number", nullable = false)
    private Integer year;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "standard_hours", nullable = false)
    private Integer standardHours;

    @Column(name = "holiday_hours", nullable = false)
    private Integer holidayHours;

    @Column(name = "approved_leave_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal approvedLeaveHours;

    @Column(name = "net_available_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal netAvailableHours;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public WeeklyAvailabilityJpaEntity() {}

    public WeeklyAvailabilityJpaEntity(Long id, Long employeeId, Integer year, Integer weekNumber,
                                       Integer standardHours, Integer holidayHours,
                                       BigDecimal approvedLeaveHours, BigDecimal netAvailableHours) {
        this.id = id;
        this.employeeId = employeeId;
        this.year = year;
        this.weekNumber = weekNumber;
        this.standardHours = standardHours;
        this.holidayHours = holidayHours;
        this.approvedLeaveHours = approvedLeaveHours;
        this.netAvailableHours = netAvailableHours;
        this.version = 0L;
    }

    public WeeklyAvailabilityJpaEntity(Long id, Long employeeId, Integer year, Integer weekNumber,
                                       Integer standardHours, Integer holidayHours,
                                       BigDecimal approvedLeaveHours, BigDecimal netAvailableHours, Long version) {
        this.id = id;
        this.employeeId = employeeId;
        this.year = year;
        this.weekNumber = weekNumber;
        this.standardHours = standardHours;
        this.holidayHours = holidayHours;
        this.approvedLeaveHours = approvedLeaveHours;
        this.netAvailableHours = netAvailableHours;
        this.version = version != null ? version : 0L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getStandardHours() {
        return standardHours;
    }

    public void setStandardHours(Integer standardHours) {
        this.standardHours = standardHours;
    }

    public Integer getHolidayHours() {
        return holidayHours;
    }

    public void setHolidayHours(Integer holidayHours) {
        this.holidayHours = holidayHours;
    }

    public BigDecimal getApprovedLeaveHours() {
        return approvedLeaveHours;
    }

    public void setApprovedLeaveHours(BigDecimal approvedLeaveHours) {
        this.approvedLeaveHours = approvedLeaveHours;
    }

    public BigDecimal getNetAvailableHours() {
        return netAvailableHours;
    }

    public void setNetAvailableHours(BigDecimal netAvailableHours) {
        this.netAvailableHours = netAvailableHours;
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

    public void setVersion(Long version) {
        this.version = version;
    }
}
