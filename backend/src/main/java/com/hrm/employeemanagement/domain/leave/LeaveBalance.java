package com.hrm.employeemanagement.domain.leave;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity đại diện cho Quỹ ngày phép năm của nhân sự (NCL-05-CN-005).
 */
public class LeaveBalance {

    private final Long id;
    private final Long employeeId;
    private final int yearNumber;
    private BigDecimal entitledDays;      // Số ngày phép năm được hưởng
    private BigDecimal carriedOverDays;   // Số ngày phép năm trước chuyển sang

    public LeaveBalance(Long id, Long employeeId, int yearNumber, BigDecimal entitledDays, BigDecimal carriedOverDays) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.yearNumber = yearNumber;
        this.entitledDays = validateNonNegative(entitledDays, "entitledDays");
        this.carriedOverDays = validateNonNegative(carriedOverDays, "carriedOverDays");
    }

    public static LeaveBalance createDefault(Long employeeId, int yearNumber) {
        return new LeaveBalance(null, employeeId, yearNumber, BigDecimal.valueOf(12.0), BigDecimal.ZERO);
    }

    private BigDecimal validateNonNegative(BigDecimal days, String fieldName) {
        if (days == null) {
            return BigDecimal.ZERO;
        }
        if (days.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " không được nhỏ hơn 0");
        }
        return days;
    }

    public void updateEntitledDays(BigDecimal newEntitledDays) {
        this.entitledDays = validateNonNegative(newEntitledDays, "entitledDays");
    }

    public void updateCarriedOverDays(BigDecimal newCarriedOverDays) {
        this.carriedOverDays = validateNonNegative(newCarriedOverDays, "carriedOverDays");
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public int getYearNumber() {
        return yearNumber;
    }

    public BigDecimal getEntitledDays() {
        return entitledDays;
    }

    public BigDecimal getCarriedOverDays() {
        return carriedOverDays;
    }

    public BigDecimal getTotalAllocatedDays() {
        return this.entitledDays.add(this.carriedOverDays);
    }
}
