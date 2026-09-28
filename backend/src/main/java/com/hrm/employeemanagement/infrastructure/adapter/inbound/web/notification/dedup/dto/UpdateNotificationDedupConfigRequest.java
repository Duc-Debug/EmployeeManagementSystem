package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dedup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationDedupConfigRequest(
        @NotNull(message = "isEnabled không được để null")
        Boolean isEnabled,

        @NotNull(message = "dedupWindowDays không được để null")
        @Min(value = 1, message = "Cửa sổ chống trùng tối thiểu là 1 ngày")
        @Max(value = 90, message = "Cửa sổ chống trùng tối đa là 90 ngày")
        Integer dedupWindowDays,

        @NotNull(message = "scanIntervalMinutes không được để null")
        @Min(value = 5, message = "Chu kỳ quét tối thiểu là 5 phút")
        @Max(value = 1440, message = "Chu kỳ quét tối đa là 1440 phút (24 giờ)")
        Integer scanIntervalMinutes
) {
}
