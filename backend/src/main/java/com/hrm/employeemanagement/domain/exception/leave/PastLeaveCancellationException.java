package com.hrm.employeemanagement.domain.exception.leave;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Quy tắc: Chỉ hủy được nghỉ phép chưa tới ngày nghỉ.
 * Ngoại lệ ném ra khi cố gắng hủy hoặc yêu cầu hủy đơn nghỉ phép có ngày bắt đầu đã hoặc đang diễn ra.
 */
public class PastLeaveCancellationException extends DomainException {

    public PastLeaveCancellationException(String message) {
        super(message);
    }

    public static PastLeaveCancellationException alreadyStarted() {
        return new PastLeaveCancellationException("Chỉ có thể hủy đơn nghỉ phép khi ngày nghỉ chưa diễn ra.");
    }
}
