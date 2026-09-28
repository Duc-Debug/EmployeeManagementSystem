package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.time.LocalDate;

/**
 * Lệnh xác nhận xử lý/ghi nhận cảnh báo hợp đồng thuê ngoài (lưu lịch sử kiểm toán TC-04).
 */
public record AcknowledgeOutsourcedContractCommand(
        Long employeeId,
        String actionNote,
        LocalDate expectedResolutionDate
) {}
