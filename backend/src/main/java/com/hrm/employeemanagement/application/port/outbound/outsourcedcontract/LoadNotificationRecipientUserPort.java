package com.hrm.employeemanagement.application.port.outbound.outsourcedcontract;

import java.util.List;

/**
 * Outbound Port lấy danh sách người dùng nhận cảnh báo hợp đồng thuê ngoài (VT-03 và VT-05).
 */
public interface LoadNotificationRecipientUserPort {
    List<Long> findResourceManagersAndHrUserIds();
}
