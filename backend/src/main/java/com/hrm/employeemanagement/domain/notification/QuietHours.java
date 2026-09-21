package com.hrm.employeemanagement.domain.notification;

import java.time.LocalTime;
import java.util.Objects;

public record QuietHours(
        boolean enabled,
        LocalTime startTime,
        LocalTime endTime
) {
    public QuietHours {
        if (enabled) {
            Objects.requireNonNull(startTime, "startTime không được để trống khi bật chế độ yên tĩnh");
            Objects.requireNonNull(endTime, "endTime không được để trống khi bật chế độ yên tĩnh");
        }
    }

    public static QuietHours disabled() {
        return new QuietHours(false, null, null);
    }

    public static QuietHours of(boolean enabled, LocalTime startTime, LocalTime endTime) {
        if (!enabled) {
            return disabled();
        }
        return new QuietHours(true, startTime, endTime);
    }

    public boolean isInQuietHours(LocalTime targetTime) {
        if (!enabled || startTime == null || endTime == null || targetTime == null) {
            return false;
        }
        if (startTime.isBefore(endTime)) {
            // Cùng trong ngày (ví dụ 12:00 - 13:30)
            return !targetTime.isBefore(startTime) && targetTime.isBefore(endTime);
        } else {
            // Qua đêm (ví dụ 22:00 - 07:00)
            return !targetTime.isBefore(startTime) || targetTime.isBefore(endTime);
        }
    }
}
