package com.hrm.employeemanagement.domain.exception.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hrm.employeemanagement.domain.exception.DomainException;
/**
 * 
 * DailyHoursLimitExceededException:  Giới hạn giờ làm trong ngày
 */
public class DailyHoursLimitExceededException extends DomainException {

    private final LocalDate workDate;
    private final BigDecimal currentHours;
    private final BigDecimal requestedHours;

    public DailyHoursLimitExceededException(LocalDate workDate, BigDecimal currentHours, BigDecimal requestedHours) {
        super(String.format("Tổng giờ làm trong ngày %s (%s giờ) cộng thêm %s giờ vượt quá giới hạn 12 giờ/ngày theo quy tắc QTN-09.",
                workDate, currentHours, requestedHours));
        this.workDate = workDate;
        this.currentHours = currentHours;
        this.requestedHours = requestedHours;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public BigDecimal getCurrentHours() {
        return currentHours;
    }

    public BigDecimal getRequestedHours() {
        return requestedHours;
    }
}
