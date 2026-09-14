package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi phát hiện xung đột phiên bản dữ liệu (Optimistic Locking Concurrency Conflict)
 * đối với cấu hình ngưỡng cảnh báo năng lực (NCL-07-CN-004 / QTN-23).
 */
public class CapacityThresholdVersionConflictException extends DomainException {

    public CapacityThresholdVersionConflictException(String message) {
        super(message);
    }
}
