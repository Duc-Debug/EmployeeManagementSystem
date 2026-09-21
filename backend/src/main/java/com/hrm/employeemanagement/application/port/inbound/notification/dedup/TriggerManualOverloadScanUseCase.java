package com.hrm.employeemanagement.application.port.inbound.notification.dedup;

import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;

public interface TriggerManualOverloadScanUseCase {

    /**
     * Kích hoạt rà soát quá tải thủ công từ Quản trị viên (VT-06).
     * Yêu cầu xác thực và phân quyền NOTIFICATION_DEDUPLICATION_MANAGE, ghi nhận audit log khi bị từ chối.
     *
     * @param year       năm (tùy chọn)
     * @param weekNumber số tuần ISO (tùy chọn)
     * @return kết quả rà soát quá tải
     */
    OverloadScanResult triggerManualScan(Integer year, Integer weekNumber);
}
