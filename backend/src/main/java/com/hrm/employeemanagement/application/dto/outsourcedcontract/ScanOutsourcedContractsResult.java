package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.time.LocalDateTime;

/**
 * DTO kết quả chạy tác vụ rà soát thời hạn hợp đồng thuê ngoài (NCL-14-CN-003).
 *
 * @param scannedAt Thời điểm thực hiện rà soát
 * @param totalScanned Tổng số nhân sự thuê ngoài có hợp đồng được quét
 * @param totalExpiringContractsFound Tổng số hợp đồng sắp hết hạn hoặc đã quá hạn phát hiện được
 * @param notificationEventsCreated Số sự kiện thông báo cảnh báo đã tạo (mỗi hợp đồng cảnh báo tạo 1 notification event)
 * @param notificationsSent Tổng số thông báo thực tế gửi tới các người nhận (notificationEventsCreated * số người nhận)
 * @param details Thông điệp chi tiết mô tả kết quả rà soát
 */
public record ScanOutsourcedContractsResult(
        LocalDateTime scannedAt,
        int totalScanned,
        int totalExpiringContractsFound,
        int notificationEventsCreated,
        int notificationsSent,
        String details
) {
    /**
     * Constructor tương thích ngược cho các lời gọi cũ (5 tham số).
     */
    public ScanOutsourcedContractsResult(
            LocalDateTime scannedAt,
            int totalScanned,
            int totalExpiringContractsFound,
            int notificationsSent,
            String details
    ) {
        this(scannedAt, totalScanned, totalExpiringContractsFound, notificationsSent, notificationsSent, details);
    }
}
