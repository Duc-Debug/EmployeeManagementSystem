package com.hrm.employeemanagement.domain.exception.allocation;

import java.time.LocalDate;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi vi phạm quy tắc QTN-21:
 * Nhân sự thuê ngoài chỉ được phân bổ vào các tuần nằm trong khoảng hiệu lực hợp đồng.
 */
public class OutsourcedContractPeriodException extends DomainException {

    private final Long employeeId;
    private final String employeeCode;
    private final String providerName;
    private final LocalDate startDate;
    private final LocalDate contractEndDate;
    private final YearWeek yearWeek;

    public OutsourcedContractPeriodException(
            String message,
            Long employeeId,
            String employeeCode,
            String providerName,
            LocalDate startDate,
            LocalDate contractEndDate,
            YearWeek yearWeek
    ) {
        super(message);
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.providerName = providerName;
        this.startDate = startDate;
        this.contractEndDate = contractEndDate;
        this.yearWeek = yearWeek;
    }

    public OutsourcedContractPeriodException(String message) {
        this(message, null, null, null, null, null, null);
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public YearWeek getYearWeek() {
        return yearWeek;
    }
}

