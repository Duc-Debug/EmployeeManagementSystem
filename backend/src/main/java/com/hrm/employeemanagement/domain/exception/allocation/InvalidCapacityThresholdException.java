package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi dữ liệu cấu hình ngưỡng năng lực không hợp lệ (TC-02).
 */
public class InvalidCapacityThresholdException extends DomainException {

    public InvalidCapacityThresholdException(String message) {
        super(message);
    }
}
