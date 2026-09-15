package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi không tìm thấy cấu hình ngưỡng năng lực theo yêu cầu.
 */
public class CapacityThresholdNotFoundException extends DomainException {

    public CapacityThresholdNotFoundException(String message) {
        super(message);
    }
}
