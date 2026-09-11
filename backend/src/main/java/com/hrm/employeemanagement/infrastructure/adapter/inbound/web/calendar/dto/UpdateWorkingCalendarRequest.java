package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record UpdateWorkingCalendarRequest(
        @NotNull(message = "Danh sách ngày làm việc không được để trống")
        @Size(min = 7, max = 7, message = "Lịch làm việc phải khai báo đầy đủ 7 ngày trong tuần")
        List<@Valid DayScheduleRequest> days
) {
    @AssertTrue(message = "Lịch làm việc phải bao gồm đầy đủ 7 ngày trong tuần từ Thứ Hai đến Chủ Nhật và không được trùng lặp")
    public boolean isValidWeek() {
        if (days == null || days.size() != 7) {
            return false;
        }
        Set<DayOfWeek> uniqueDays = EnumSet.noneOf(DayOfWeek.class);
        for (DayScheduleRequest d : days) {
            if (d == null || d.dayOfWeek() == null) {
                return false;
            }
            if (!uniqueDays.add(d.dayOfWeek())) {
                return false;
            }
        }
        return uniqueDays.size() == 7;
    }

    public record DayScheduleRequest(
            @NotNull(message = "Thứ trong tuần không được để trống")
            DayOfWeek dayOfWeek,
            boolean isWorkingDay
    ) {}

    public List<WorkingCalendarDayDto> toDtos() {
        return days.stream()
                .map(d -> new WorkingCalendarDayDto(d.dayOfWeek(), d.isWorkingDay()))
                .toList();
    }
}
