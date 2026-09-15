package com.hrm.employeemanagement.application.dto.allocation.idleness;

import com.hrm.employeemanagement.domain.allocation.idleness.ProlongedIdlenessPolicy;

/**
 * Query DTO rà soát nhân sự nhàn rỗi kéo dài (NCL-07-CN-006).
 */
public record ProlongedIdlenessQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks,
        Integer consecutiveThreshold,
        String search,
        String status,
        Integer page,
        Integer size
) {
    public ProlongedIdlenessQuery {
        durationWeeks = (durationWeeks != null && durationWeeks > 0) ? durationWeeks : 4;
        // TC-01: Ngưỡng cảnh báo chuỗi tuần nhàn rỗi được chuẩn hóa theo Domain Policy (3 tuần liên tiếp)
        consecutiveThreshold = ProlongedIdlenessPolicy.DEFAULT_CONSECUTIVE_WEEKS;
        status = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";
        page = (page != null && page >= 0) ? page : 0;
        size = (size != null && size > 0) ? size : 20;
    }

    public ProlongedIdlenessQuery(
            Long orgUnitId,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            Integer consecutiveThreshold,
            String search,
            Integer page,
            Integer size
    ) {
        this(orgUnitId, fromYear, fromWeek, durationWeeks, consecutiveThreshold, search, "ALL", page, size);
    }

    public ProlongedIdlenessQuery(
            Long orgUnitId,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            Integer consecutiveThreshold,
            String search
    ) {
        this(orgUnitId, fromYear, fromWeek, durationWeeks, consecutiveThreshold, search, "ALL", 0, 20);
    }
}
