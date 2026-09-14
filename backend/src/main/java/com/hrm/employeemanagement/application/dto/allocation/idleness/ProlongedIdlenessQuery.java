package com.hrm.employeemanagement.application.dto.allocation.idleness;

/**
 * Query DTO rà soát nhân sự nhàn rỗi kéo dài (NCL-07-CN-006).
 */
public record ProlongedIdlenessQuery(
        Long orgUnitId,
        Integer fromYear,
        Integer fromWeek,
        Integer durationWeeks,
        Integer consecutiveThreshold,
        String search
) {
    public ProlongedIdlenessQuery {
        durationWeeks = (durationWeeks != null && durationWeeks > 0) ? durationWeeks : 4;
        consecutiveThreshold = (consecutiveThreshold != null && consecutiveThreshold > 0) ? consecutiveThreshold : 3;
    }
}
