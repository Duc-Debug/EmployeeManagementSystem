package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "hours_deducted", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursDeducted;

    public LeaveRequestJpaEntity() {}

    public LeaveRequestJpaEntity(Long id, Long employeeId, LocalDate startDate, LocalDate endDate,
                                 String status, BigDecimal hoursDeducted) {
        this.id = id;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.hoursDeducted = hoursDeducted;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getHoursDeducted() {
        return hoursDeducted;
    }
}
