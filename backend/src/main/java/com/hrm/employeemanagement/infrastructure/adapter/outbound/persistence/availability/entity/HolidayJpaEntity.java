package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
public class HolidayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "working_hours_deducted", nullable = false)
    private Integer workingHoursDeducted = 8;

    public HolidayJpaEntity() {}

    public HolidayJpaEntity(Long id, LocalDate holidayDate, String name, Integer workingHoursDeducted) {
        this.id = id;
        this.holidayDate = holidayDate;
        this.name = name;
        this.workingHoursDeducted = workingHoursDeducted != null ? workingHoursDeducted : 8;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getName() {
        return name;
    }

    public Integer getWorkingHoursDeducted() {
        return workingHoursDeducted;
    }
}
