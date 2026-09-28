package com.hrm.employeemanagement.domain.outsourcedcontract;

/**
 * Trạng thái thời hạn hợp đồng thuê ngoài dựa trên ngưỡng rà soát 30 ngày.
 */
public enum OutsourcedContractStatus {
    /**
     * Hợp đồng còn thời hạn an toàn (> 30 ngày).
     */
    ACTIVE_SAFE,

    /**
     * Hợp đồng sắp hết hạn (trong vòng 30 ngày tới, ví dụ 25 ngày theo TC-01).
     */
    EXPIRING_SOON,

    /**
     * Hợp đồng đã hết hạn (quá hạn).
     */
    EXPIRED
}
