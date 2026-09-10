package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;
import jakarta.validation.constraints.NotEmpty;

import java.time.DayOfWeek;
import java.util.List;

public record UpdateWorkingCalendarRequest(
        @NotEmpty(message = "Danh sách ngày làm việc không được để trống")
        List<DayScheduleRequest> days
) {
    public record DayScheduleRequest(
            DayOfWeek dayOfWeek,
            boolean isWorkingDay
    ) {}

    public List<WorkingCalendarDayDto> toDtos() {
        return days.stream()
                .map(d -> new WorkingCalendarDayDto(d.dayOfWeek(), d.isWorkingDay()))
                .toList();
    }
}
