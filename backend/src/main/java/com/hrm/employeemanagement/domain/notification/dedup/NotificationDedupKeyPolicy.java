package com.hrm.employeemanagement.domain.notification.dedup;

import java.util.Objects;

/**
 * Domain Policy sinh khóa chống gửi trùng thông báo theo QTN-19 và NCL-11-CN-003:
 * dedup_key = {eventType}:{targetEntityType}:{targetEntityId}:{yearWeek}:{recipientUserId}
 */
public final class NotificationDedupKeyPolicy {

    private NotificationDedupKeyPolicy() {
    }

    public static String buildKey(
            String eventType,
            String targetEntityType,
            String targetEntityId,
            String yearWeek,
            Long recipientUserId
    ) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType không được để trống khi tạo khóa chống trùng");
        }
        if (targetEntityType == null || targetEntityType.isBlank()) {
            throw new IllegalArgumentException("targetEntityType không được để trống khi tạo khóa chống trùng");
        }
        if (targetEntityId == null || targetEntityId.isBlank()) {
            throw new IllegalArgumentException("targetEntityId không được để trống khi tạo khóa chống trùng");
        }
        if (yearWeek == null || yearWeek.isBlank()) {
            throw new IllegalArgumentException("yearWeek không được để trống khi tạo khóa chống trùng");
        }
        Objects.requireNonNull(recipientUserId, "recipientUserId không được null khi tạo khóa chống trùng");

        return String.format("%s:%s:%s:%s:USER:%d",
                eventType.trim().toUpperCase(),
                targetEntityType.trim().toUpperCase(),
                targetEntityId.trim(),
                yearWeek.trim(),
                recipientUserId
        );
    }
}
