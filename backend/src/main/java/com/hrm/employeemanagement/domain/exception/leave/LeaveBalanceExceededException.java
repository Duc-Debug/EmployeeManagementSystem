package com.hrm.employeemanagement.domain.exception.leave;

import com.hrm.employeemanagement.domain.exception.DomainException;

import java.math.BigDecimal;

/**
 * TC-02: Ngoại lệ ném ra khi gửi đơn nghỉ phép năm vượt quá số ngày phép còn lại.
 */
public class LeaveBalanceExceededException extends DomainException {

    private final BigDecimal remainingDays;
    private final BigDecimal requestedDays;

    public LeaveBalanceExceededException(BigDecimal remainingDays, BigDecimal requestedDays) {
        super(String.format(
                "Số ngày xin nghỉ (%s ngày) vượt quá quỹ ngày phép năm còn lại (%s ngày). Vui lòng chọn loại nghỉ không hưởng lương.",
                requestedDays != null ? requestedDays.stripTrailingZeros().toPlainString() : "0",
                remainingDays != null ? remainingDays.stripTrailingZeros().toPlainString() : "0"
        ));
        this.remainingDays = remainingDays;
        this.requestedDays = requestedDays;
    }

    public BigDecimal getRemainingDays() {
        return remainingDays;
    }

    public BigDecimal getRequestedDays() {
        return requestedDays;
    }
}
