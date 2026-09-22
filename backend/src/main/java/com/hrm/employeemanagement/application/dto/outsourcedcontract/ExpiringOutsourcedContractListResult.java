package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.util.Collections;
import java.util.List;

/**
 * DTO danh sách các hợp đồng thuê ngoài sắp hết hạn hoặc quá hạn phục vụ màn hình theo dõi.
 */
public record ExpiringOutsourcedContractListResult(
        int totalExpiringContracts,
        List<ExpiringOutsourcedContractResult> items
) {
    public static ExpiringOutsourcedContractListResult of(List<ExpiringOutsourcedContractResult> items) {
        if (items == null || items.isEmpty()) {
            return new ExpiringOutsourcedContractListResult(0, Collections.emptyList());
        }
        return new ExpiringOutsourcedContractListResult(items.size(), items);
    }
}
