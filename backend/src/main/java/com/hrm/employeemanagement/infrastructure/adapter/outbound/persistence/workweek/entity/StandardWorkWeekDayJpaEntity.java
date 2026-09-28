package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "standard_work_week_days",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sww_config_day", columnNames = {"config_id", "day_of_week"})
        }
)
public class StandardWorkWeekDayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private StandardWorkWeekConfigJpaEntity config;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek;

    @Column(name = "is_working_day", nullable = false)
    private Boolean isWorkingDay = true;

    @Column(name = "working_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal workingHours = BigDecimal.valueOf(8.00);

    public StandardWorkWeekDayJpaEntity() {}

    public StandardWorkWeekDayJpaEntity(StandardWorkWeekConfigJpaEntity config, String dayOfWeek, Boolean isWorkingDay, BigDecimal workingHours) {
        this.config = config;
        this.dayOfWeek = dayOfWeek;
        this.isWorkingDay = isWorkingDay;
        this.workingHours = workingHours;
    }

    public Long getId() {
        return id;
    }

    public StandardWorkWeekConfigJpaEntity getConfig() {
        return config;
    }

    public void setConfig(StandardWorkWeekConfigJpaEntity config) {
        this.config = config;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getIsWorkingDay() {
        return isWorkingDay;
    }

    public void setIsWorkingDay(Boolean isWorkingDay) {
        this.isWorkingDay = isWorkingDay;
    }

    public BigDecimal getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(BigDecimal workingHours) {
        this.workingHours = workingHours;
    }
}

