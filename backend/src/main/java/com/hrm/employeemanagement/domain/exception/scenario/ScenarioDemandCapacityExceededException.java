package com.hrm.employeemanagement.domain.exception.scenario;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi tổng nhu cầu của kịch bản vượt quá năng lực (capacity) khả dụng của nhân sự,
 * và người dùng không bật tùy chọn chấp nhận phân bổ một phần (allowPartialFulfillment).
 */
public class ScenarioDemandCapacityExceededException extends DomainException {
    public ScenarioDemandCapacityExceededException(String message) {
        super(message);
    }
}

