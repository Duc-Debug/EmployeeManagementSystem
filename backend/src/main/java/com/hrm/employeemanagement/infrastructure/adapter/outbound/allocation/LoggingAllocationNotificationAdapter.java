package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.AllocationNotificationPort;

/**
 * Adapter ghi nhận và mô phỏng gửi thông báo cho QLDA khi có điều chỉnh phân bổ (QTN-15, K6).
 */
@Component
public class LoggingAllocationNotificationAdapter implements AllocationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAllocationNotificationAdapter.class);

    @Override
    public String notifyAllocationAdjusted(Long projectId, Long managerId, String message) {
        log.info("[NOTIFICATION] Gửi thông báo đến QLDA (Employee ID: {}) của Dự án (ID: {}): {}",
                managerId != null ? managerId : "CHƯA_GÁN_QLDA",
                projectId,
                message);
        return managerId != null ? String.valueOf(managerId) : null;
    }
}
