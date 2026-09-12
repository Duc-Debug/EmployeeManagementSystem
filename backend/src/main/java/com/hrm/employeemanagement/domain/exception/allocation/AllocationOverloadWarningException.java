package com.hrm.employeemanagement.domain.exception.allocation;

import java.math.BigDecimal;
import com.hrm.employeemanagement.domain.exception.DomainException;

public class AllocationOverloadWarningException extends DomainException {

    private final BigDecimal availableHours;
    private final BigDecimal allocatedHours;
    private final BigDecimal overloadHours;

    public AllocationOverloadWarningException(
            String message,
            BigDecimal availableHours,
            BigDecimal allocatedHours,
            BigDecimal overloadHours
    ) {
        super(message);
        this.availableHours = availableHours;
        this.allocatedHours = allocatedHours;
        this.overloadHours = overloadHours;
    }

    public BigDecimal getAvailableHours() {
        return availableHours;
    }

    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    public BigDecimal getOverloadHours() {
        return overloadHours;
    }
}