package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "working_calendar_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_working_calendar_day", columnNames = {"company_code", "day_of_week"})
        }
)
public class WorkingCalendarConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 50)
    private String companyCode = "DEFAULT";

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek;

    @Column(name = "is_working_day", nullable = false)
    private Boolean isWorkingDay = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkingCalendarConfigJpaEntity() {}

    public WorkingCalendarConfigJpaEntity(Long id, String companyCode, String dayOfWeek, Boolean isWorkingDay) {
        this.id = id;
        this.companyCode = companyCode != null ? companyCode : "DEFAULT";
        this.dayOfWeek = dayOfWeek;
        this.isWorkingDay = isWorkingDay != null ? isWorkingDay : true;
    }

    public Long getId() {
        return id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public Boolean getIsWorkingDay() {
        return isWorkingDay;
    }

    public void setIsWorkingDay(Boolean isWorkingDay) {
        this.isWorkingDay = isWorkingDay;
    }
}
