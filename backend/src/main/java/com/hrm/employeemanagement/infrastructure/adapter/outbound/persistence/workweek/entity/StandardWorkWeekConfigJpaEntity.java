package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "standard_work_week_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sww_scope_key", columnNames = {"scope_key"})
        }
)
public class StandardWorkWeekConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType = "COMPANY";

    @Column(name = "scope_key", nullable = false, length = 100)
    private String scopeKey;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "capacity_unit", nullable = false, length = 20)
    private String capacityUnit = "HOURS";

    @Column(name = "week_start_day", nullable = false, length = 20)
    private String weekStartDay = "MONDAY";

    @Column(name = "standard_hours_per_day", nullable = false, precision = 4, scale = 2)
    private BigDecimal standardHoursPerDay = BigDecimal.valueOf(8.00);

    @Column(name = "standard_hours_per_week", nullable = false, precision = 5, scale = 2)
    private BigDecimal standardHoursPerWeek = BigDecimal.valueOf(40.00);

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StandardWorkWeekDayJpaEntity> days = new ArrayList<>();

    // The audit user may be absent for seeded rows or become null via FK ON DELETE SET NULL.
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public StandardWorkWeekConfigJpaEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    public String getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(String weekStartDay) {
        this.weekStartDay = weekStartDay;
    }

    public BigDecimal getStandardHoursPerDay() {
        return standardHoursPerDay;
    }

    public void setStandardHoursPerDay(BigDecimal standardHoursPerDay) {
        this.standardHoursPerDay = standardHoursPerDay;
    }

    public BigDecimal getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public void setStandardHoursPerWeek(BigDecimal standardHoursPerWeek) {
        this.standardHoursPerWeek = standardHoursPerWeek;
    }

    public List<StandardWorkWeekDayJpaEntity> getDays() {
        return days;
    }

    public void setDays(List<StandardWorkWeekDayJpaEntity> days) {
        this.days = days;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

