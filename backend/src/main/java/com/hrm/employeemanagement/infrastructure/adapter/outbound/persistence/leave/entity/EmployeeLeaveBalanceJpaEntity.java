package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_balances")
public class EmployeeLeaveBalanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year_number", nullable = false)
    private int yearNumber;

    @Column(name = "entitled_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal entitledDays;

    @Column(name = "carried_over_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal carriedOverDays;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public EmployeeLeaveBalanceJpaEntity() {}

    public EmployeeLeaveBalanceJpaEntity(Long id, Long employeeId, int yearNumber,
                                         BigDecimal entitledDays, BigDecimal carriedOverDays) {
        this.id = id;
        this.employeeId = employeeId;
        this.yearNumber = yearNumber;
        this.entitledDays = entitledDays;
        this.carriedOverDays = carriedOverDays;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public int getYearNumber() { return yearNumber; }
    public void setYearNumber(int yearNumber) { this.yearNumber = yearNumber; }
    public BigDecimal getEntitledDays() { return entitledDays; }
    public void setEntitledDays(BigDecimal entitledDays) { this.entitledDays = entitledDays; }
    public BigDecimal getCarriedOverDays() { return carriedOverDays; }
    public void setCarriedOverDays(BigDecimal carriedOverDays) { this.carriedOverDays = carriedOverDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
