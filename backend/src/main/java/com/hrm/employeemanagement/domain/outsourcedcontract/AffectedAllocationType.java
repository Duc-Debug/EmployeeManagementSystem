package com.hrm.employeemanagement.domain.outsourcedcontract;

/**
 * Phân loại mức độ ảnh hưởng của phân bổ đối với thời hạn hợp đồng thuê ngoài theo quy tắc QTN-21.
 */
public enum AffectedAllocationType {
    /**
     * Tuần phân bổ chứa ngày hết hạn hợp đồng (ngày kết thúc tuần sau ngày hết hạn hợp đồng),
     * dẫn đến các ngày làm việc sau ngày hết hạn bị vắt qua thời hạn hợp đồng.
     */
    SPANS_OVER_EXPIRY,

    /**
     * Tuần phân bổ bắt đầu hoàn toàn sau ngày hết hạn hợp đồng (vi phạm QTN-21).
     */
    AFTER_EXPIRY
}
